package no.nav.helse.sporenstreks.web.dto

import io.mockk.every
import io.mockk.mockkStatic
import no.nav.helse.TestData
import no.nav.helse.sporenstreks.domene.Arbeidsgiverperiode
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.valiktor.ConstraintViolationException
import org.valiktor.i18n.toMessage
import java.time.LocalDate
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RefusjonsKravDtoTestIkkeEtterGjenåpning {
    @BeforeAll
    fun setup() {
        mockkStatic(Class.forName("java.time.LocalDate").kotlin)
        every { LocalDate.now() } returns LocalDate.parse("2022-07-15")
    }

    @Test
    fun `Kan ikke søke refusjonskrav etter gjenåpning`() {
        try {
            RefusjonskravDto(
                TestData.validIdentitetsnummer,
                TestData.validOrgNr,
                setOf(
                    Arbeidsgiverperiode(
                        LocalDate.of(2022, 7, 1),
                        LocalDate.of(2022, 7, 10),
                        2, 2.3
                    )
                )
            ).validate(TestData.arbeidsForhold)
        } catch (ex: ConstraintViolationException) {
            val validationError = ex.constraintViolations
                .map { "${it.constraint.name}: ${it.toMessage().message}" }
                .first()
            assertEquals("RefusjonsdagerInnenforGjenaapningConstraint: Det kan ikke kreves refusjon fra og med 01.07.2022", validationError)
        }
    }

    @Test
    fun `Kan søke refusjonskrav før gjenåpning`() {
        RefusjonskravDto(
            TestData.validIdentitetsnummer,
            TestData.validOrgNr,
            setOf(
                Arbeidsgiverperiode(
                    LocalDate.of(2022, 6, 30),
                    LocalDate.of(2022, 7, 10),
                    2, 2.3
                )
            )
        ).validate(TestData.arbeidsForhold)
    }

    @Test
    fun `Kan søke refusjonskrav hvis en periode er før gjenåpning`() {
        RefusjonskravDto(
            TestData.validIdentitetsnummer,
            TestData.validOrgNr,
            setOf(
                Arbeidsgiverperiode(
                    LocalDate.of(2022, 6, 30),
                    LocalDate.of(2022, 7, 10),
                    2, 2.3
                ),
                Arbeidsgiverperiode(
                    LocalDate.of(2022, 7, 11),
                    LocalDate.of(2022, 7, 13),
                    2, 2.3
                )
            )
        ).validate(TestData.arbeidsForhold)
    }
}
