package no.nav.helse.sporenstreks.web.dto

import io.mockk.every
import io.mockk.mockkStatic
import no.nav.helse.TestData
import no.nav.helse.arbeidsgiver.integrasjoner.aareg.*
import no.nav.helse.sporenstreks.domene.Arbeidsgiverperiode
import no.nav.helse.sporenstreks.domene.Refusjonskrav
import no.nav.helse.sporenstreks.web.api.dto.validation.validerArbeidsforhold
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate
import java.time.LocalDate.of
import java.time.LocalDateTime

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AaregConstraintsTest {
    @BeforeAll
    fun setup() {
        mockkStatic(Class.forName("java.time.LocalDate").kotlin)
        every { LocalDate.now() } returns LocalDate.parse("2022-01-20")
    }

    @Test
    fun `Gyldig arbeidsforhold og gyldig krav`() {
        val arbeidsgiver = Arbeidsgiver("AS", "123456785")
        val opplysningspliktig = Opplysningspliktig("AS", "1212121212")
        val arbeidsForhold = Arbeidsforhold(
            arbeidsgiver,
            opplysningspliktig,
            emptyList(),
            Ansettelsesperiode(
                Periode(
                    of(2021, 1, 1),
                    null
                )
            ),
            LocalDateTime.now()
        )

        val refusjonskravDto = Refusjonskrav(
            TestData.opprettetAv,
            TestData.validIdentitetsnummer,
            TestData.validOrgNr,
            setOf(
                Arbeidsgiverperiode(
                    of(2022, 1, 1),
                    of(2022, 1, 7),
                    2, 2.3
                )
            )
        )

        validerArbeidsforhold(
            listOf(arbeidsForhold),
            refusjonskravDto
        )
    }
}
