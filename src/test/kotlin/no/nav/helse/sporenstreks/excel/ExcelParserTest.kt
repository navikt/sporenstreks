package no.nav.helse.sporenstreks.excel

import io.mockk.every
import io.mockk.mockk
import no.nav.helse.TestData
import no.nav.helse.sporenstreks.auth.Authorizer
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ExcelParserTest {

    val validFile = ExcelParserTest::class.java.classLoader.getResourceAsStream("koronasykepengerefusjon_nav_TESTFILE.xlsx");
    val invalidFile = ExcelParserTest::class.java.classLoader.getResourceAsStream("koronasykepengerefusjon_nav_ERRORFILE.xlsx");

    val authorizerMock = mockk<Authorizer>()

    @BeforeEach
    fun setup() {
        every { authorizerMock.hasAccess(any(), any()) } returns true
    }

    @Test
    fun shouldParseValidFileOkay() {
        val workbook: Workbook = XSSFWorkbook(validFile)
        val result = ExcelParser(authorizerMock).parseAndValidateExcelContent(workbook, TestData.validIdentitetsnummer)

        assertThat(result.refusjonskrav.size).isEqualTo(10)
        assertThat(result.errors).isEqualTo(0)
    }

    @Test
    fun `Parseren skal gi feil på riktig rad og kolonne`() {
        val workbook: Workbook = XSSFWorkbook(invalidFile)
        val result = ExcelParser(authorizerMock).parseAndValidateExcelContent(workbook, TestData.validIdentitetsnummer)

        assertThat(result.refusjonskrav.size).isEqualTo(0)
        assertThat(result.errors.size).isEqualTo(9)

        val rowErrors = result.errors.groupBy { it.rowNumber }

        assertThat(rowErrors[11]?.size).isEqualTo(1)
        assertThat(rowErrors[11]?.get(0)?.column).isEqualTo("Fødselsnummer")



    }
}