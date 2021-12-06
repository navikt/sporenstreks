package no.nav.helse.sporenstreks.web.api.validation

import no.nav.helse.sporenstreks.domene.Arbeidsgiverperiode
import no.nav.helse.sporenstreks.web.dto.validation.arbeidsgiverBetalerForDager
import org.junit.jupiter.api.Test
import org.valiktor.validate
import java.time.LocalDate

class RefusjonsDagerConstraintTest {

    data class RefusjonskravDtoTestClass(val perioder: Set<Arbeidsgiverperiode>)

    @Test
    fun `Gyldige perioder før 1 oktober`() {
        val testRefusjon = RefusjonskravDtoTestClass(
            perioder = setOf(
                Arbeidsgiverperiode(
                    LocalDate.of(2021, 5, 1),
                    LocalDate.of(2021, 5, 6),
                    4,
                    2590.8
                ),
                Arbeidsgiverperiode(
                    LocalDate.of(2021, 5, 8),
                    LocalDate.of(2021, 5, 15),
                    5,
                    2590.8
                ),
            )
        )

        validate(testRefusjon) {
            validate(RefusjonskravDtoTestClass::perioder).arbeidsgiverBetalerForDager(3, LocalDate.of(2020, 3, 16))
        }
    }

    @Test
    fun `Ugyldige perioder etter 1 oktober før 1 desember`() {
        val testRefusjon = RefusjonskravDtoTestClass(
            perioder = setOf(
                Arbeidsgiverperiode(
                    LocalDate.of(2021, 11, 1),
                    LocalDate.of(2021, 11, 6),
                    4,
                    2590.8
                ),
                Arbeidsgiverperiode(
                    LocalDate.of(2021, 11, 8),
                    LocalDate.of(2021, 11, 15),
                    5,
                    2590.8
                ),
            )
        )
        validationShouldFailFor(RefusjonskravDtoTestClass::perioder) {
            validate(testRefusjon) {
                validate(RefusjonskravDtoTestClass::perioder).arbeidsgiverBetalerForDager(3, LocalDate.of(2020, 3, 16))
            }
        }
    }

    @Test
    fun `Gyldige perioder etter 1 desember`() {
        val testRefusjon = RefusjonskravDtoTestClass(
            perioder = setOf(
                Arbeidsgiverperiode(
                    LocalDate.of(2021, 12, 1),
                    LocalDate.of(2021, 12, 6),
                    4,
                    2590.8
                ),
                Arbeidsgiverperiode(
                    LocalDate.of(2021, 12, 8),
                    LocalDate.of(2021, 12, 15),
                    5,
                    2590.8
                ),
            )
        )
        validate(testRefusjon) {
            validate(RefusjonskravDtoTestClass::perioder).arbeidsgiverBetalerForDager(5, LocalDate.of(2021, 12, 1))
        }
    }
}
