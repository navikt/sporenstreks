package no.nav.helse.sporenstreks.excel

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.TestData
import no.nav.helse.arbeidsgiver.web.auth.AltinnAuthorizer
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ExcelParserTest {

    val validFile = ExcelParserTest::class.java.classLoader.getResourceAsStream("koronasykepengerefusjon_nav_TESTFILE.xlsx")
    val invalidFile = ExcelParserTest::class.java.classLoader.getResourceAsStream("koronasykepengerefusjon_nav_ERRORFILE.xlsx")

    val authorizerMock = mockk<AltinnAuthorizer>()

    @BeforeEach
    fun setup() {
        every { authorizerMock.hasAccess(any(), any()) } returns true
    }

    @Test
    fun `Gyldig fil skal ikke gi noen feil`() {
        val workbook: Workbook = XSSFWorkbook(validFile)
        val result = ExcelParser(authorizerMock).parseAndValidateExcelContent(workbook, TestData.validIdentitetsnummer)

        verify(atLeast = 1) { authorizerMock.hasAccess(TestData.validIdentitetsnummer, any()) }
        assertThat(result.refusjonskrav.size).isEqualTo(11)
        assertThat(result.errors.size).isEqualTo(0)
    }

    @Test
    fun `Parseren skal gi feil på riktig rad og kolonne`() {
        val workbook: Workbook = XSSFWorkbook(invalidFile)
        val result = ExcelParser(authorizerMock).parseAndValidateExcelContent(workbook, TestData.validIdentitetsnummer)

        assertThat(result.refusjonskrav.size).isEqualTo(1)
        assertThat(result.errors.size).isEqualTo(8)

        val rowErrors = result.errors.groupBy { it.rowNumber }

        assertThat(rowErrors[12]?.size).isEqualTo(1)
        assertThat(rowErrors[12]?.get(0)?.column).isEqualTo("Fødselsnummer")

        assertThat(rowErrors[13]?.size).isEqualTo(1)
        assertThat(rowErrors[13]?.get(0)?.column).isEqualTo("Virksomhetsnummer")

        assertThat(rowErrors[14]?.size).isEqualTo(1)
        assertThat(rowErrors[14]?.get(0)?.column).isEqualTo("Fra og med")

        assertThat(rowErrors[15]?.size).isEqualTo(1)
        assertThat(rowErrors[15]?.get(0)?.column).isEqualTo("Til og med")

        assertThat(rowErrors[16]?.size).isEqualTo(3)
        assertThat(rowErrors[16]?.get(0)?.column).isEqualTo("Arbeidsgiverperioden (fom+tom)")
        assertThat(rowErrors[16]?.get(1)?.column).isEqualTo("Arbeidsgiverperioden (fom+tom)")
        assertThat(rowErrors[16]?.get(2)?.column).isEqualTo("Arbeidsgiverperioden (fom+tom)")

        assertThat(rowErrors[17]?.size).isEqualTo(1)
        assertThat(rowErrors[17]?.get(0)?.column).isEqualTo("Beløp")
    }

    @Test
    fun `Har man ikke tilgang til virksomheten skal man få feil`() {
        val workbook: Workbook = XSSFWorkbook(validFile)
        every { authorizerMock.hasAccess(any(), any()) } returns false

        val result = ExcelParser(authorizerMock).parseAndValidateExcelContent(workbook, TestData.validIdentitetsnummer)

        assertThat(result.errors.size).isEqualTo(11)
        assertThat(result.errors.all { it.column.equals("Virksomhetsnummer") && it.message.contains("tilgang") })
    }
}
