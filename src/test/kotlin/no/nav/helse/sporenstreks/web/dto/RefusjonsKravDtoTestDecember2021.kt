package no.nav.helse.sporenstreks.web.dto

import io.mockk.every
import io.mockk.mockkStatic
import no.nav.helse.TestData
import no.nav.helse.sporenstreks.domene.Arbeidsgiverperiode
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.valiktor.ConstraintViolationException
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RefusjonsKravDtoTestDecember2021 {
    @BeforeAll
    fun setup() {
        mockkStatic(Class.forName("java.time.LocalDate").kotlin)
        every { LocalDate.now() } returns LocalDate.parse("2022-04-22")
    }

    @Test
    fun `Refusjonskrav med arbeidsperiode som starter 4 dager før ordning er gyldig`() {
        RefusjonskravDto(
            TestData.validIdentitetsnummer,
            TestData.validOrgNr,
            setOf(
                Arbeidsgiverperiode(
                    LocalDate.of(2021, 12, 28),
                    LocalDate.of(2022, 1, 10),
                    9, 300.0
                )
            )
        ).validate(TestData.arbeidsForhold)
    }
    @Test
    fun `Refusjonskrav med arbeidsperiode som starter 5 dager før ordning er gyldig`() {
        RefusjonskravDto(
            TestData.validIdentitetsnummer,
            TestData.validOrgNr,
            setOf(
                Arbeidsgiverperiode(
                    LocalDate.of(2021, 12, 27),
                    LocalDate.of(2022, 1, 10),
                    9, 300.0
                )
            )
        ).validate(TestData.arbeidsForhold)
    }

    @Test
    fun `Det kan ikke kreves refusjon for dager før ordning`() {
        Assertions.assertThatExceptionOfType(ConstraintViolationException::class.java).isThrownBy {

            RefusjonskravDto(
                TestData.validIdentitetsnummer,
                TestData.validOrgNr,
                setOf(
                    Arbeidsgiverperiode(
                        LocalDate.of(2021, 12, 28),
                        LocalDate.of(2022, 1, 2),
                        5, 200.3
                    )
                )
            ).validate(TestData.arbeidsForhold)
        }
    }

    @Test
    fun `Refusjonskrav med arbeidsperiode som starter 6 dager før ordning er IKKE gyldig`() {
        Assertions.assertThatExceptionOfType(ConstraintViolationException::class.java).isThrownBy {

            RefusjonskravDto(
                TestData.validIdentitetsnummer,
                TestData.validOrgNr,
                setOf(
                    Arbeidsgiverperiode(
                        LocalDate.of(2021, 12, 26),
                        LocalDate.of(2022, 1, 10),
                        9, 200.3
                    )
                )
            ).validate(TestData.arbeidsForhold)
        }
    }
}
