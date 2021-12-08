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
internal class RefusjonsKravDtoTestNyPeriode {
    @BeforeAll
    fun setup() {
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns LocalDate.parse("2021-12-07")
    }

    @Test
    fun `Refusjonskrav i ny periode validerer OK`() {
        RefusjonskravDto(
            TestData.validIdentitetsnummer,
            TestData.validOrgNr,
            setOf(
                Arbeidsgiverperiode(
                    LocalDate.of(2021, 12, 1),
                    LocalDate.of(2021, 12, 7),
                    2, 2.3
                )
            )
        )
    }

    @Test
    fun `Maks refusjonsdager er totalperiode minus 5 dager i ny periode`() {
        Assertions.assertThatExceptionOfType(ConstraintViolationException::class.java).isThrownBy {
            RefusjonskravDto(
                TestData.validIdentitetsnummer,
                TestData.validOrgNr,
                setOf(
                    Arbeidsgiverperiode(
                        LocalDate.of(2021, 12, 1),
                        LocalDate.of(2021, 12, 6),
                        2, 2000.0
                    )
                )
            )
        }
    }

    @Test
    fun `Det kan ikke kreves refusjon fra og med 1 oktober 2021 til og med 30 november 2021`() {
        Assertions.assertThatExceptionOfType(ConstraintViolationException::class.java).isThrownBy {
            RefusjonskravDto(
                TestData.validIdentitetsnummer,
                TestData.validOrgNr,
                setOf(
                    Arbeidsgiverperiode(
                        LocalDate.of(2021, 10, 1),
                        LocalDate.of(2021, 10, 8),
                        1, 2000.0
                    )
                )
            )
        }
    }

/*
    @Test
    fun `Det kan ikke kreves refusjon for perioder lenger enn 3 måneder siden i gammel periode`() {
        Assertions.assertThatExceptionOfType(ConstraintViolationException::class.java).isThrownBy {
            RefusjonskravDto(
                TestData.validIdentitetsnummer,
                TestData.validOrgNr,
                setOf(
                    Arbeidsgiverperiode(
                        LocalDate.of(2021, 9, 1),
                        LocalDate.of(2021, 9, 6),
                        2, 2.3
                    )
                )
            )
        }
    }

 */
}
