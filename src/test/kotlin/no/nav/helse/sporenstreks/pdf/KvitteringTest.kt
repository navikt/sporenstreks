package no.nav.helse.sporenstreks.pdf

import com.google.common.io.Files
import no.nav.helse.TestData
import no.nav.helse.sporenstreks.domene.Arbeidsgiverperiode
import no.nav.helse.sporenstreks.domene.Refusjonskrav
import no.nav.helse.sporenstreks.domene.RefusjonskravStatus
import org.junit.jupiter.api.Test
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertTrue

internal class KvitteringTest {

    @Test
    fun lagPDF() {
        val refusjonskrav = Refusjonskrav(
            opprettetAv = TestData.opprettetAv,
            identitetsnummer = TestData.validIdentitetsnummer,
            virksomhetsnummer = TestData.validOrgNr,
            perioder = setOf(
                Arbeidsgiverperiode(
                    LocalDate.of(2022, 4, 1),
                    LocalDate.of(2022, 4, 5),
                    2,
                    4500800.50
                ),
                Arbeidsgiverperiode(
                    LocalDate.of(2022, 4, 5),
                    LocalDate.of(2022, 4, 10),
                    4,
                    1220800.50
                )
            ),
            opprettet = LocalDateTime.now(),
            status = RefusjonskravStatus.MOTTATT,
            referansenummer = 12345
        )
        val kv = PDFGenerator()
        val ba = kv.lagPDF(refusjonskrav, TestData.virksomhetsNavn)
//        val file = File("kvittering_vanlig.pdf")
        val file = File.createTempFile("kvittering_vanlig", "pdf")
        Files.write(ba, file)
        assertTrue { file.exists() }
    }

    @Test
    fun norskeBokstaver() {
        val kv = PDFGenerator()
        val refusjonskrav = Refusjonskrav(
            opprettetAv = TestData.opprettetAv,
            identitetsnummer = TestData.validIdentitetsnummer,
            virksomhetsnummer = TestData.validOrgNr,
            perioder = setOf(
                Arbeidsgiverperiode(
                    LocalDate.of(2022, 4, 1),
                    LocalDate.of(2022, 4, 5),
                    2,
                    250.50
                )
            ),
            opprettet = LocalDateTime.now(),
            status = RefusjonskravStatus.MOTTATT,
            referansenummer = 12345
        )
        val ba = kv.lagPDF(refusjonskrav, TestData.virksomhetsNavn)
//        val file = File("kvittering_spesialtegn.pdf")
        val file = File.createTempFile("kvittering_spesialtegn", "pdf")
        Files.write(ba, file)
        assertTrue { file.exists() }
    }
}
