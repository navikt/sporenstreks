package no.nav.helse.sporenstreks.pdf

import no.nav.helse.TestData
import no.nav.helse.sporenstreks.domene.Arbeidsgiverperiode
import no.nav.helse.sporenstreks.web.dto.RefusjonskravDto
import org.junit.jupiter.api.Test

import wiremock.com.google.common.io.Files
import java.io.File
import java.time.LocalDate
import kotlin.test.assertTrue

internal class KvitteringTest {

    @Test
    fun lagPDF() {
        val innhold = Innhold(
                navn = "Kari Nordmann",
                refusjonskrav = RefusjonskravDto(
                        identitetsnummer = TestData.validIdentitetsnummer,
                        virksomhetsnummer = TestData.validOrgNr,
                        perioder = setOf(Arbeidsgiverperiode(
                                LocalDate.of(2020, 4,1),
                                LocalDate.of(2020, 4,5),
                                2
                        ), Arbeidsgiverperiode(
                                LocalDate.of(2020, 4, 5),
                                LocalDate.of(2020, 4, 10),
                                4
                        )),
                        beloep = 4500800.50
                )
        )
        val kv = Kvittering()
        val ba = kv.lagPDF(innhold)
        val file = File("kvittering_vanlig.pdf")
//        val file = File.createTempFile("kvittering_vanlig", "pdf")
        Files.write(ba, file)
        assertTrue { file.exists() }
    }

    @Test
    fun norskeBokstaver() {
        val kv = Kvittering()
        val innhold = Innhold(
                navn = "ZæøåÆØÅAaÁáBbCcČčDdĐđEeFfGgHhIiJjKkLlMmNnŊŋOoPpRrSsŠšTtŦŧUuVvZzŽžéôèÉöüäÖÜÄ.'\\-/%§!?@_()+:;,=\"&",
                refusjonskrav = RefusjonskravDto(
                        identitetsnummer = TestData.validIdentitetsnummer,
                        virksomhetsnummer = TestData.validOrgNr,
                        perioder = setOf(Arbeidsgiverperiode(
                                LocalDate.of(2020, 4,1),
                                LocalDate.of(2020, 4,5),
                                2
                        )),
                        beloep = 4500800.50
                )
        )
        val ba = kv.lagPDF(innhold)
        val file = File("kvittering_spesialtegn.pdf")
//        val file = File.createTempFile("kvittering_spesialtegn", "pdf")
        Files.write(ba, file)
        assertTrue { file.exists() }
    }
}