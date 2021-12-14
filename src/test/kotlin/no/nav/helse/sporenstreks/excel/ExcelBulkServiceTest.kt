package no.nav.helse.sporenstreks.excel

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.TestData
import no.nav.helse.sporenstreks.domene.Refusjonskrav
import no.nav.helse.sporenstreks.service.RefusjonskravService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class ExcelBulkServiceTest {

    val excelFile = ExcelParserTest::class.java.classLoader.getResourceAsStream("koronasykepengerefusjon_nav_TESTFILE.xlsx")
    val parserMock = mockk<ExcelParser>()
    val serviceMock = mockk<RefusjonskravService>()

    @Test
    internal suspend fun `thrower ved feil i excelarket`() {
        val bulkservice = ExcelBulkService(serviceMock, parserMock)
        coEvery { parserMock.parseAndValidateExcelContent(any(), TestData.validIdentitetsnummer) } returns ExcelParser.ExcelParsingResult(emptyList(), setOf(ExcelFileRowError(1, "test", "test")))
        assertThrows<ExcelFileParsingException> { bulkservice.processExcelFile(excelFile, TestData.validIdentitetsnummer) }
    }

    @Test
    internal suspend fun `Lagrer til databasen ved feilfri parsing`() {
        val bulkservice = ExcelBulkService(serviceMock, parserMock)
        val refusjonskrabParsedFromFile = listOf(
            Refusjonskrav(
                TestData.opprettetAv,
                TestData.validIdentitetsnummer,
                TestData.validOrgNr,
                emptySet()
            )
        )

        coEvery { parserMock.parseAndValidateExcelContent(any(), TestData.validIdentitetsnummer) } returns ExcelParser.ExcelParsingResult(refusjonskrabParsedFromFile, emptySet())
        val refernceNumber = 123
        every { serviceMock.bulkInsert(refusjonskrabParsedFromFile) } returns listOf(refernceNumber)

        bulkservice.processExcelFile(excelFile, TestData.validIdentitetsnummer)

        verify(exactly = 1) { serviceMock.bulkInsert(refusjonskrabParsedFromFile) }
    }
}
