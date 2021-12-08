package no.nav.helse.sporenstreks.excel

import no.nav.helse.sporenstreks.metrics.INNKOMMENDE_REFUSJONSKRAV_COUNTER
import no.nav.helse.sporenstreks.service.RefusjonskravService
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.slf4j.LoggerFactory
import java.io.InputStream

class ExcelBulkService(private val service: RefusjonskravService, private val parser: ExcelParser) {
    private val maxRowNum: Int = 5000
    private val log = LoggerFactory.getLogger(ExcelBulkService::class.java)

    fun processExcelFile(file: InputStream, opprettetAvIdentitetsnummer: String) {
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

        val første = parsingResult.refusjonskrav.first().gammelOrdning
        if (parsingResult.refusjonskrav.any { it.gammelOrdning != første }) throw ExcelFileParsingException("Det er ikke mulig å kreve refusjon for perioder som gjelder gammel ordning i samme skjema som ny ordning")

        log.info("Lagrer ${parsingResult.refusjonskrav.size} krav")
        val referenceNumbers = service.bulkInsert(parsingResult.refusjonskrav)
        log.info("Lagret  ${parsingResult.refusjonskrav.size} krav")
        INNKOMMENDE_REFUSJONSKRAV_COUNTER.inc(parsingResult.refusjonskrav.size.toDouble())
    }

    companion object {
        const val startDataRowAt = 10
        const val referenceNumberColumnIndex = 6
    }
}
