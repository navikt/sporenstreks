package no.nav.helse.sporenstreks.web.dto.validation

import no.nav.helse.sporenstreks.domene.Arbeidsgiverperiode
import org.junit.jupiter.api.Test
import org.valiktor.validate
import java.time.LocalDate

class RefusjonsDagerConstraintTest {

    data class RefusjonskravDtoTestClass(val perioder: Set<Arbeidsgiverperiode>)

    @Test
    fun `Ugyldige perioder før nedstenging`() {
        val testRefusjon = RefusjonskravDtoTestClass(
            perioder = setOf(
                Arbeidsgiverperiode(
                    LocalDate.of(2019, 5, 1),
                    LocalDate.of(2019, 5, 6),
                    4,
                    2590.8
                ),
                Arbeidsgiverperiode(
                    LocalDate.of(2019, 5, 8),
                    LocalDate.of(2019, 5, 15),
                    5,
                    2590.8
                ),
            )
        )
        validationShouldFailFor(RefusjonskravDtoTestClass::perioder) {
            validate(testRefusjon) {
                validate(RefusjonskravDtoTestClass::perioder).arbeidsgiverBetalerForDager(LocalDate.of(2020, 3, 16))
            }
        }
    }

    @Test
    fun `Ugyldige perioder for over 3 måneder tilbake i tid`() {
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
        validationShouldFailFor(RefusjonskravDtoTestClass::perioder) {
            validate(testRefusjon) {
                validate(RefusjonskravDtoTestClass::perioder).arbeidsgiverBetalerForDager(LocalDate.of(2020, 3, 16))
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
            validate(RefusjonskravDtoTestClass::perioder).arbeidsgiverBetalerForDager(LocalDate.of(2020, 3, 6))
        }
    }

    @Test
    fun `Gyldig periode som starter før 1 desember`() {
        val testRefusjon = RefusjonskravDtoTestClass(
            perioder = setOf(
                Arbeidsgiverperiode(
                    LocalDate.of(2021, 11, 22),
                    LocalDate.of(2021, 12, 2),
                    1,
                    2590.8
                )
            )
        )
        validate(testRefusjon) {
            validate(RefusjonskravDtoTestClass::perioder).arbeidsgiverBetalerForDager(LocalDate.of(2020, 3, 6))
        }
    }

    @Test
    fun `Ugyldig periode som starter før 1 desember`() {
        val testRefusjon = RefusjonskravDtoTestClass(
            perioder = setOf(
                Arbeidsgiverperiode(
                    LocalDate.of(2021, 11, 22),
                    LocalDate.of(2021, 12, 2),
                    5,
                    2590.8
                )
            )
        )
        validationShouldFailFor(RefusjonskravDtoTestClass::perioder) {
            validate(testRefusjon) {
                validate(RefusjonskravDtoTestClass::perioder).arbeidsgiverBetalerForDager(LocalDate.of(2020, 3, 6))
            }
        }
    }
}
