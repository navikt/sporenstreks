package no.nav.helse.sporenstreks.excel

import no.nav.helse.sporenstreks.auth.Authorizer
import no.nav.helse.sporenstreks.excel.ExcelBulkService.Companion.startDataRow
import no.nav.helse.sporenstreks.domene.Arbeidsgiverperiode
import no.nav.helse.sporenstreks.domene.Refusjonskrav
import no.nav.helse.sporenstreks.web.dto.RefusjonskravDto
import no.nav.helse.sporenstreks.web.dto.validation.getContextualMessage
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFCell
import org.valiktor.ConstraintViolation
import org.valiktor.ConstraintViolationException
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.*
import javax.ws.rs.ForbiddenException
import kotlin.collections.ArrayList
import kotlin.collections.HashSet


class ExcelParser(private val authorizer: Authorizer) {
    fun parseAndValidateExcelContent(workbook: Workbook, opprettetAv: String): ExcelParsingResult {
        val sheet = workbook.getSheetAt(0)

        val refusjonsKrav = ArrayList<Refusjonskrav>()
        val errorRows = HashSet<ExcelFileRowError>()

        var currentDataRow = startDataRow
        val parseRunId = UUID.randomUUID().toString()
        var row :Row? = sheet.getRow(currentDataRow)

        while (row != null && row.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).stringCellValue != "") {
            try {
                val krav = extractRefusjonsKravFromExcelRow(row, opprettetAv, parseRunId)
                refusjonsKrav.add(krav)
            } catch (ex: ForbiddenException) {
                errorRows.add(ExcelFileRowError(
                        currentDataRow+1,
                        "virksomhetsnummer",
                        "Du har ikke korrekte tilganger for denne virksomheten")
                )
            } catch(valErr: ConstraintViolationException) {
                errorRows.addAll(
                        valErr.constraintViolations.map { ExcelFileRowError(
                            currentDataRow+1, it.property, it.getContextualMessage())
                        }
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

        return ExcelParsingResult(refusjonsKrav, errorRows)
    }

    private fun extractRefusjonsKravFromExcelRow(row: Row, opprettetAv: String, correlationId: String): Refusjonskrav {
        // extract values
        val identitetsnummer = row.extract(0, "Fødselsnummer")
        val virksomhetsNummer = row.extract(1, "Virksomhetsnummer")
        val fom = row.extractLocalDate(2, "Fra og med")
        val tom = row.extractLocalDate(3, "Til og med")
        val beloep = row.extractDouble(4, "Beløp")

        // authorize the use
        if (!authorizer.hasAccess(opprettetAv, virksomhetsNummer)) {
            throw ForbiddenException("Du har ikke tilgang til tjenesten for virksomhet '$virksomhetsNummer'")
        }

        // create DTO instance for validation
        val refusjonskrav = RefusjonskravDto(
                identitetsnummer,
                virksomhetsNummer,
                setOf(Arbeidsgiverperiode(fom, tom, Period.between(fom, tom.plusDays(1)).days - 3, beloep))
        )

        // map to domain instance for insertion into Database
        return Refusjonskrav(
                opprettetAv,
                refusjonskrav.identitetsnummer,
                refusjonskrav.virksomhetsnummer,
                refusjonskrav.perioder,
                kilde = "XLSX-$correlationId"
        )
    }

    private fun Row.extract(cellNum: Int, columnName: String): String {
        try {
            val cell = this.getCell(cellNum)
            return when (cell.cellType) {
                CellType.BLANK -> throw CellValueExtractionException("Kan ikke være blank", columnName)
                CellType.ERROR -> throw CellValueExtractionException("Feil i celletypen", columnName)
                CellType._NONE -> throw CellValueExtractionException("Feil i celletypen", columnName)
                CellType.NUMERIC -> (cell as XSSFCell).rawValue
                CellType.STRING -> cell.stringCellValue.trim()
                CellType.FORMULA -> (cell as XSSFCell).rawValue
                CellType.BOOLEAN -> throw CellValueExtractionException("Feil i celletypen, må være Tekst", columnName)
                else -> throw CellValueExtractionException("Uventet feil ved uthenting av verdien", columnName)
            }
        } catch (ex: Exception) {
            throw CellValueExtractionException(columnName, "En uventet feil oppsto under uthenting av celleverdien. Sjekk celletypen og påpass at den er Tekst", ex)
        }
    }

    private fun Row.extractLocalDate(cellNum: Int, columnName: String): LocalDate {
        val value = this.extract(cellNum, columnName)
        try {
            return LocalDate.parse(value.trim(), DateTimeFormatter.ofPattern("dd.MM.uuuu"))
        } catch (ex: Exception) {
            throw CellValueExtractionException(columnName, "Feil ved lesing av dato. Påse at datoformatet er riktig.", ex)
        }
    }

    private fun Row.extractDouble(cellNum: Int, columnName: String): Double {
        val value = this.extract(cellNum, columnName)
        try {
            return value.toDouble()
        } catch (ex: Exception) {
            throw CellValueExtractionException(columnName, "Feil ved lesing av tall. Påse at formatet er riktig.", ex)
        }
    }

    data class ExcelParsingResult(val refusjonskrav: List<Refusjonskrav>, val errors: Set<ExcelFileRowError>) {
        fun hasErrors(): Boolean = errors.isNotEmpty()
    }
}

