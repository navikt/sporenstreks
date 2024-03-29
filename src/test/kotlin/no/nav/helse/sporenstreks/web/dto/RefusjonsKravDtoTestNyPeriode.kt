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
import org.valiktor.i18n.toMessage
import java.time.LocalDate
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RefusjonsKravDtoTestNyPeriode {
    @BeforeAll
    fun setup() {
        mockkStatic(Class.forName("java.time.LocalDate").kotlin)
        every { LocalDate.now() } returns LocalDate.parse("2022-03-15")
    }

    @Test
    fun `Refusjonskrav i ny periode validerer OK`() {
        RefusjonskravDto(
            TestData.validIdentitetsnummer,
            TestData.validOrgNr,
            setOf(
                Arbeidsgiverperiode(
                    LocalDate.of(2022, 2, 1),
                    LocalDate.of(2022, 2, 7),
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

    @Test
    fun `Det kan ikke kreves refusjon for perioder lenger enn 3 måneder siden i ny periode`() {
        try {
            RefusjonskravDto(
                TestData.validIdentitetsnummer,
                TestData.validOrgNr,
                setOf(
                    Arbeidsgiverperiode(
                        LocalDate.of(2021, 12, 1),
                        LocalDate.of(2021, 12, 10),
                        2, 2.3
                    )
                )
            )
        } catch (ex: ConstraintViolationException) {
            val validationError = ex.constraintViolations
                .map { "${it.constraint.name}: ${it.toMessage().message}" }
                .first()
            assertEquals("RefusjonsdagerInnenforAntallMånederConstraint: Det kan ikke kreves refusjon for lenger enn 3 måneder siden.", validationError)
        }
    }

    @Test
    fun `Kan ikke søke før 1 desember i ny periode`() {
        Assertions.assertThatExceptionOfType(ConstraintViolationException::class.java).isThrownBy {
            RefusjonskravDto(
                TestData.validIdentitetsnummer,
                TestData.validOrgNr,
                setOf(
                    Arbeidsgiverperiode(
                        LocalDate.of(2021, 11, 30),
                        LocalDate.of(2021, 12, 10),
                        2, 2.3
                    )
                )
            ).validate(TestData.arbeidsForhold)
        }
    }

    @Test
    fun `Kan søke 1 desember i ny periode`() {
        RefusjonskravDto(
            TestData.validIdentitetsnummer,
            TestData.validOrgNr,
            setOf(
                Arbeidsgiverperiode(
                    LocalDate.of(2021, 12, 1),
                    LocalDate.of(2021, 12, 10),
                    2, 2.3
                )
            )
        ).validate(TestData.arbeidsForhold)
    }

    @Test
    fun `Arbeidsforhold slåes sammen`() {
        RefusjonskravDto(
            TestData.validIdentitetsnummer,
            TestData.validOrgNr,
            setOf(
                Arbeidsgiverperiode(
                    LocalDate.of(2021, 12, 30),
                    LocalDate.of(2022, 1, 10),
                    2, 2.3
                )
            )
        ).validate(TestData.flereArbeidsForholdISammeOrg)
    }
}
