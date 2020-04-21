package no.nav.helse.sporenstreks.excel

import no.nav.helse.sporenstreks.db.RefusjonskravRepository
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.InputStream

class ExcelBulkService(private val db: RefusjonskravRepository, private val parser: ExcelParser) {
    private val maxRowNum: Int = 5000
    private val log = LoggerFactory.getLogger(ExcelBulkService::class.java)

    fun processExcelFile(file: InputStream, opprettetAvIdentitetsnummer: String): ByteArray {
        log.info("Starter prosseseringen av Excel fil")
        val workbook: Workbook = XSSFWorkbook(file)

        val parsingResult = parser.parseAndValidateExcelContent(workbook, opprettetAvIdentitetsnummer)

        log.info("Hentet ut ${parsingResult.refusjonskrav.size} krav og ${parsingResult.errors.size} feil")
        if (parsingResult.hasErrors()) {
            throw ExcelFileParsingException("Det er feil i filen. Rett feilene nedenfor.", parsingResult.errors)
        }

        if (parsingResult.refusjonskrav.isEmpty() && !parsingResult.hasErrors()) {
            throw ExcelFileParsingException("Det er ingen rader i filen, malen må fylles ut.")
        }

        if (parsingResult.refusjonskrav.size > maxRowNum) {
            throw ExcelFileParsingException("Det er for mange rader i filen. Maks er $maxRowNum")
        }

        log.info("Lagrer ${parsingResult.refusjonskrav.size} krav")
        val referenceNumbers = db.bulkInsert(parsingResult.refusjonskrav)
        log.info("Lagret  ${parsingResult.refusjonskrav.size} krav")

        // Håndtere at vi har lagret men skriving tilbake til filen feiler
        val sheet = workbook.getSheetAt(0);
        sheet.getRow(startDataRowAt-1)
                .getCell(referenceNumberColumnIndex, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK)
                .setCellValue("Referansenummer hos NAV")

        referenceNumbers.forEachIndexed { i, refNr ->
            sheet.getRow(startDataRowAt + i)
                    .getCell(referenceNumberColumnIndex, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK)
                    .setCellValue(refNr.toString())
        }

        val stream = ByteArrayOutputStream()
        workbook.write(stream)
        return stream.toByteArray()
    }

    companion object {
        const val startDataRowAt = 10
        const val referenceNumberColumnIndex = 6
    }
}