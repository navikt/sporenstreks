package no.nav.helse.sporenstreks.bulk

import no.nav.helse.sporenstreks.auth.Authorizer
import no.nav.helse.sporenstreks.db.RefusjonskravRepository
import no.nav.helse.sporenstreks.domene.Arbeidsgiverperiode
import no.nav.helse.sporenstreks.domene.Refusjonskrav
import no.nav.helse.sporenstreks.web.dto.RefusjonskravDto
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFCell
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.lang.IllegalArgumentException
import java.time.LocalDate
import javax.ws.rs.ForbiddenException


class ExcelBulkService(private val db: RefusjonskravRepository, private val authorizer: Authorizer) {
    val log = LoggerFactory.getLogger(ExcelBulkService::class.java)

    fun processExcelFile(file: InputStream, opprettetAvIdentitetsnummer: String)  {
        log.info("Starter prosseseringen av Excel fil")
        val workbook: Workbook = XSSFWorkbook(file)
        val sheet = workbook.getSheetAt(0);

        val refusjonsKrav = HashSet<Refusjonskrav>()
        val errors = HashSet<ExcelFileRowError>()

        val startDataRow = 11
        var currentDataRow = startDataRow
        var row :Row? = sheet.getRow(currentDataRow)

        while (row != null && row.getCell(0).stringCellValue != "") {
            try {
                val krav = extractRefusjonsKravFromExcelRow(row, opprettetAvIdentitetsnummer)
                refusjonsKrav.add(krav)
            } catch (ex: Exception) {
                errors.add(ExcelFileRowError(
                    currentDataRow+1,
                        "",
                        ex.message ?: "Ukjent feil")
                )
            } finally {
                row = sheet.getRow(++currentDataRow)
            }
        }

        log.info("Hentet ut ${refusjonsKrav.size} krav og ${errors.size} feil")
        if (errors.isNotEmpty()) {
            throw ExcelFileParsinException(errors)
        }

        log.info("Lagrer ${refusjonsKrav.size} krav")

        // TODO: bruk en spesialisert metode med transaksjon for dette
        refusjonsKrav.forEach { db.insert(it) }

        log.info("Lagret  ${refusjonsKrav.size} krav")
        log.info("${refusjonsKrav.first()}")

    }

    fun extractRefusjonsKravFromExcelRow(row: Row, opprettetAvIdentitetsnummer: String): Refusjonskrav {
        // extract values
        val identitetsnummer = row.getCell(0).extractCellValueAsString()
        val virksomhetsNummer = row.getCell(1).extractCellValueAsString()
        val fom = LocalDate.parse(row.getCell(2).extractCellValueAsString())
        val tom = LocalDate.parse(row.getCell(3).extractCellValueAsString())
        val antallDager = row.getCell(4).extractCellValueAsString().toDouble().toInt()
        val beloep = row.getCell(5).extractCellValueAsString().toDouble()

        // authorize the use
        if (!authorizer.hasAccess(opprettetAvIdentitetsnummer, virksomhetsNummer)) {
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
                opprettetAvIdentitetsnummer,
                refusjonskrav.identitetsnummer,
                refusjonskrav.virksomhetsnummer,
                refusjonskrav.perioder,
                kilde = "EXCEL"
        )
    }

    private fun Cell.extractCellValueAsString(): String {
        return when (this.cellType) {
            CellType.BLANK -> ""
            CellType.ERROR -> "error"
            CellType._NONE -> "feil i celletypen"
            CellType.NUMERIC -> (this as XSSFCell).rawValue
            CellType.STRING -> this.stringCellValue.trim()
            CellType.FORMULA -> (this as XSSFCell).rawValue
            CellType.BOOLEAN -> "feil celletype"
            else -> ""
        }
    }

}

class ExcelFileParsinException(val errors: Set<ExcelFileRowError>) : Exception() {

}

data class ExcelFileRowError(
    val rowNumber: Int,
    val column: String,
    val message: String
)