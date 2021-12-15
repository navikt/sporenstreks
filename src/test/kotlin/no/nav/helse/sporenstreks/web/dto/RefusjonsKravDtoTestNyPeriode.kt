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
        mockkStatic(Class.forName("java.time.LocalDate").kotlin)
        every { LocalDate.now() } returns LocalDate.parse("2022-01-20")
    }

    @Test
    fun `Refusjonskrav i ny periode validerer OK`() {
        RefusjonskravDto(
            TestData.validIdentitetsnummer,
            TestData.validOrgNr,
            setOf(
                Arbeidsgiverperiode(
                    LocalDate.of(2022, 1, 1),
                    LocalDate.of(2022, 1, 7),
                    2, 2.3
                )
            )
        ).validate(TestData.arbeidsForhold)
    }

    @Test
    fun `Maks refusjonsdager er totalperiode minus 5 dager i ny periode`() {
        Assertions.assertThatExceptionOfType(ConstraintViolationException::class.java).isThrownBy {
            RefusjonskravDto(
                TestData.validIdentitetsnummer,
                TestData.validOrgNr,
                setOf(
                    Arbeidsgiverperiode(
                        LocalDate.of(2022, 1, 1),
                        LocalDate.of(2022, 1, 6),
                        2, 2000.0
                    )
                )
            ).validate(TestData.arbeidsForhold)
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
