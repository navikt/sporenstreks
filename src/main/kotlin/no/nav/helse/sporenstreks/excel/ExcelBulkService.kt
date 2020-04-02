package no.nav.helse.sporenstreks.excel

import no.nav.helse.sporenstreks.db.RefusjonskravRepository
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.io.OutputStream

class ExcelBulkService(private val db: RefusjonskravRepository, private val parser: ExcelParser) {
    private val maxRowNum: Int = 5000
    private val log = LoggerFactory.getLogger(ExcelBulkService::class.java)

    fun processExcelFile(file: InputStream, opprettetAvIdentitetsnummer: String, outputStream: OutputStream) {
        log.info("Starter prosseseringen av Excel fil")
        val workbook: Workbook = XSSFWorkbook(file)

        val parsingResult = parser.parseAndValidateExcelContent(workbook, opprettetAvIdentitetsnummer)

        log.info("Hentet ut ${parsingResult.refusjonskrav.size} krav og ${parsingResult.errors.size} feil")
        if (parsingResult.hasErrors()) {
            throw ExcelFileParsingException("Det er feil i filen. Rett feilene nedenfor.", parsingResult.errors)
        }

        if (parsingResult.refusjonskrav.size > maxRowNum) {
            throw ExcelFileParsingException("Det er for mange rader i filen. Maks er $maxRowNum")
        }

        log.info("Lagrer ${parsingResult.refusjonskrav.size} krav")
        val referenceNumbers = db.bulkInsert(parsingResult.refusjonskrav)

        // Håndtere at vi har lagret men skriving tilbake til filen feiler
        val sheet = workbook.getSheetAt(0);
        referenceNumbers.forEachIndexed { i, refNr ->
            sheet.getRow(startDataRow + i)
                    .getCell(6, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK)
                    .setCellValue(refNr.toString())
        }

         workbook.write(outputStream)
        log.info("Lagret  ${parsingResult.refusjonskrav.size} krav")
    }

    companion object {
        const val startDataRow = 11
    }
}