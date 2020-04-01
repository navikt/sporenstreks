package no.nav.helse.sporenstreks.bulk

import no.nav.helse.sporenstreks.auth.Authorizer
import no.nav.helse.sporenstreks.db.RefusjonskravRepository
import no.nav.helse.sporenstreks.domene.Arbeidsgiverperiode
import no.nav.helse.sporenstreks.domene.Refusjonskrav
import no.nav.helse.sporenstreks.web.dto.RefusjonskravDto
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFCell
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.slf4j.LoggerFactory
import org.valiktor.ConstraintViolation
import org.valiktor.ConstraintViolationException
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate
import java.util.*
import javax.ws.rs.ForbiddenException
import kotlin.collections.ArrayList
import kotlin.collections.HashSet


class ExcelBulkService(private val db: RefusjonskravRepository, private val authorizer: Authorizer) {
    private val maxRowNum: Int = 5000
    val log = LoggerFactory.getLogger(ExcelBulkService::class.java)

    fun processExcelFile(file: InputStream, opprettetAvIdentitetsnummer: String, outputStream: OutputStream, excelUploadId: String = UUID.randomUUID().toString()) {
        log.info("Starter prosseseringen av Excel fil")
        val workbook: Workbook = XSSFWorkbook(file)

        val sheet = workbook.getSheetAt(0);

        val refusjonsKrav = ArrayList<Refusjonskrav>()
        val errorRows = HashSet<ExcelFileRowError>()

        val startDataRow = 11
        var currentDataRow = startDataRow
        var row :Row? = sheet.getRow(currentDataRow)

        while (row != null && row.getCell(0).stringCellValue != "") {
            try {
                val krav = extractRefusjonsKravFromExcelRow(row, opprettetAvIdentitetsnummer, excelUploadId)
                refusjonsKrav.add(krav)
            } catch (ex: ForbiddenException) {
                errorRows.add(ExcelFileRowError(
                        currentDataRow+1,
                        "virksomhetsnummer",
                        "Du har ikke korrekte tilganger for denne virksomheten")
                )
            } catch (ex: CellValueExtractionException) {
                errorRows.add(ExcelFileRowError(
                        currentDataRow+1,
                        ex.columnName,
                        ex.message ?: "Ukjent feil")
                )
            } finally {
                row = sheet.getRow(++currentDataRow)
            }
        }

        log.info("Hentet ut ${refusjonsKrav.size} krav og ${errorRows.size} feil")
        if (errorRows.isNotEmpty()) {
            throw ExcelFileParsingException("Det er feil i filen.", errorRows)
        }

        if (refusjonsKrav.size > maxRowNum) {
            throw ExcelFileParsingException("Det er for mange rader i filen. Maks er $maxRowNum")
        }

        log.info("Lagrer ${refusjonsKrav.size} krav")

        val referansenummere = db.bulkInsert(refusjonsKrav)

        referansenummere.forEachIndexed { i, refNr ->
            sheet.getRow(startDataRow + i).getCell(6, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).setCellValue(refNr.toString())
        }

         workbook.write(outputStream)

        log.info("Lagret  ${refusjonsKrav.size} krav")
        log.info("${refusjonsKrav.first()}")

    }

    fun extractRefusjonsKravFromExcelRow(row: Row, opprettetAv: String, excelUploadId: String): Refusjonskrav {
        // extract values
        val identitetsnummer = row.extract(0, "identitetsnummer")
        val virksomhetsNummer = row.extract(1, "virksomhetsnummer")
        val fom = LocalDate.parse(row.extract(2, "fom"))
        val tom = LocalDate.parse(row.extract(3, "tom"))
        val antallDager = row.extract(4, "antall dager").toDouble().toInt()
        val beloep = row.extract(5, "beløp").toDouble()

        // authorize the use
        if (!authorizer.hasAccess(opprettetAv, virksomhetsNummer)) {
            throw ForbiddenException("Har ikke tilgang til tjenesten for virksomhet '$virksomhetsNummer'")
        }

        // create DTO instance for validation
        val refusjonskrav = RefusjonskravDto(
                identitetsnummer,
                virksomhetsNummer,
                setOf(Arbeidsgiverperiode(fom, tom, antallDager, beloep))
        )

        // map to domain instance for insertion into Database
        return Refusjonskrav(
                opprettetAv,
                refusjonskrav.identitetsnummer,
                refusjonskrav.virksomhetsnummer,
                refusjonskrav.perioder,
                kilde = "XLSX-$excelUploadId"
        )
    }

    private fun Row.extract(cellNum: Int, columnName: String): String {
        try {
            val cell = this.getCell(cellNum)
            return when (cell.cellType) {
                CellType.BLANK -> ""
                CellType.ERROR -> "error"
                CellType._NONE -> "feil i celletypen"
                CellType.NUMERIC -> (cell as XSSFCell).rawValue
                CellType.STRING -> cell.stringCellValue.trim()
                CellType.FORMULA -> (cell as XSSFCell).rawValue
                CellType.BOOLEAN -> "feil celletype"
                else -> ""
            }
        } catch (ex: Exception) {
            throw CellValueExtractionException(columnName, ex)
        }
    }
}

class ExcelFileParsingException(message: String, val errors: Set<ExcelFileRowError> = emptySet()) : Exception(message)
class CellValueExtractionException(val columnName: String, cause: Exception) : Exception(cause)

data class ExcelFileRowError(
    val rowNumber: Int,
    val column: String,
    val message: String,
    val validationErrors: Set<ConstraintViolation> = emptySet()
)