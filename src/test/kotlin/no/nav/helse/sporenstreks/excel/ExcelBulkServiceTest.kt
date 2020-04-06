package no.nav.helse.sporenstreks.excel

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.TestData
import no.nav.helse.sporenstreks.db.RefusjonskravRepository
import no.nav.helse.sporenstreks.domene.Refusjonskrav
import no.nav.helse.sporenstreks.excel.ExcelBulkService.Companion.referenceNumberColumnIndex
import no.nav.helse.sporenstreks.excel.ExcelBulkService.Companion.startDataRowAt
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.ByteArrayInputStream

internal class ExcelBulkServiceTest {

    val excelFile = ExcelParserTest::class.java.classLoader.getResourceAsStream("koronasykepengerefusjon_nav_TESTFILE.xlsx");
    val parserMock = mockk<ExcelParser>()
    val dbMock = mockk<RefusjonskravRepository>()


    @Test
    internal fun `thrower ved feil i excelarket`() {
        val bulkservice = ExcelBulkService(dbMock, parserMock)
        every { parserMock.parseAndValidateExcelContent(any(), TestData.validIdentitetsnummer) } returns ExcelParser.ExcelParsingResult(emptyList(), setOf(ExcelFileRowError(1,"test","test")))

        assertThrows<ExcelFileParsingException> { bulkservice.processExcelFile(excelFile, TestData.validIdentitetsnummer) }
    }

    @Test
    internal fun `Lagrer til databasen og setter inn referansenummere i excelarket ved feilfri parsing`() {
        val bulkservice = ExcelBulkService(dbMock, parserMock)
        val refusjonskrabParsedFromFile = listOf(Refusjonskrav(
                TestData.validIdentitetsnummer,
                TestData.validOrgNr,
                emptySet()
        ))

        every { parserMock.parseAndValidateExcelContent(any(), TestData.validIdentitetsnummer) } returns ExcelParser.ExcelParsingResult(refusjonskrabParsedFromFile, emptySet())
        val refernceNumber = 123
        every { dbMock.bulkInsert(refusjonskrabParsedFromFile) } returns listOf(refernceNumber)

        val excelWorkbookWithReferenceNumbers = bulkservice.processExcelFile(excelFile, TestData.validIdentitetsnummer)

        verify(exactly = 1) { dbMock.bulkInsert(refusjonskrabParsedFromFile) }

        val workbook: Workbook = XSSFWorkbook(ByteArrayInputStream(excelWorkbookWithReferenceNumbers))
        val sheet = workbook.getSheetAt(0)
        val refNumberCell = sheet.getRow(startDataRowAt).getCell(referenceNumberColumnIndex)

        assertThat(refNumberCell.stringCellValue).isEqualTo(refernceNumber.toString())
    }
}



