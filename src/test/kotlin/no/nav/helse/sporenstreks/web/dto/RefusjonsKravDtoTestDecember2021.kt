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
        every { LocalDate.now() } returns LocalDate.parse("2022-05-22")
    }

    @Test
    fun `Refusjonskrav med arbeidsperiode som starter 4 dager før ordning er gyldig`() {
        RefusjonskravDto(
            TestData.validIdentitetsnummer,
            TestData.validOrgNr,
            setOf(
                Arbeidsgiverperiode(
                    LocalDate.of(2022, 1, 28),
                    LocalDate.of(2022, 2, 10),
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
                    LocalDate.of(2022, 1, 27),
                    LocalDate.of(2022, 2, 10),
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
                        LocalDate.of(2022, 1, 28),
                        LocalDate.of(2022, 2, 2),
                        5, 200.3
                    )
                )
            ).validate(TestData.arbeidsForhold)
        }
    }
    @Test
    fun `Det skal være mulig å kreve refusjon for alle dager fra og med fristen`() {

        RefusjonskravDto(
            TestData.validIdentitetsnummer,
            TestData.validOrgNr,
            setOf(
                Arbeidsgiverperiode(
                    LocalDate.of(2022, 1, 24),
                    LocalDate.of(2022, 2, 4),
                    4, 200.3
                )
            )
        ).validate(TestData.arbeidsForhold)
    }

    @Test
    fun `Refusjonskrav med arbeidsperiode som starter 16 dager før ordning er IKKE gyldig`() {
        Assertions.assertThatExceptionOfType(ConstraintViolationException::class.java).isThrownBy {

            RefusjonskravDto(
                TestData.validIdentitetsnummer,
                TestData.validOrgNr,
                setOf(
                    Arbeidsgiverperiode(
                        LocalDate.of(2022, 1, 16),
                        LocalDate.of(2022, 2, 1),
                        9, 200.3
                    )
                )
            ).validate(TestData.arbeidsForhold)
        }
    }
    @Test
    fun `Refusjonskrav med arbeidsperiode som slutter før ordning er IKKE gyldig`() {
        Assertions.assertThatExceptionOfType(ConstraintViolationException::class.java).isThrownBy {

            RefusjonskravDto(
                TestData.validIdentitetsnummer,
                TestData.validOrgNr,
                setOf(
                    Arbeidsgiverperiode(
                        LocalDate.of(2022, 1, 17),
                        LocalDate.of(2022, 1, 31),
                        9, 200.3
                    )
                )
            ).validate(TestData.arbeidsForhold)
        }
    }
}
