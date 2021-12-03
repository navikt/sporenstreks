package no.nav.helse.sporenstreks.web.api.validation

import no.nav.helse.TestData
import no.nav.helse.sporenstreks.domene.Arbeidsgiverperiode
import no.nav.helse.sporenstreks.web.dto.RefusjonskravDto
import no.nav.helse.sporenstreks.web.dto.validation.arbeidsgiverBetalerForDager
import org.junit.jupiter.api.Test
import org.valiktor.validate
import java.time.LocalDate

class RefusjonsDagerConstraintTest {

    @Test
    fun `Gyldige perioder før 1 oktober`() {
        val perioder = setOf<Arbeidsgiverperiode>(
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
        val krav = RefusjonskravDto(
            TestData.validIdentitetsnummer,
            TestData.validOrgNr,
            perioder
        )
        validate(krav) {
            validate(RefusjonskravDto::perioder).arbeidsgiverBetalerForDager(3, LocalDate.of(2020, 3, 16))
        }
    }

    @Test
    fun `Ugyldige perioder etter 1 oktober før 1 desember`() {
        val perioder = setOf<Arbeidsgiverperiode>(
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
        val krav = RefusjonskravDto(
            TestData.validIdentitetsnummer,
            TestData.validOrgNr,
            perioder
        )
        validate(krav) {
            validate(RefusjonskravDto::perioder).arbeidsgiverBetalerForDager(3, LocalDate.of(2020, 3, 16))
        }
    }
    @Test
    fun `Gyldige perioder etter 1 desember`() {
        val perioder = setOf<Arbeidsgiverperiode>(
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
        val krav = RefusjonskravDto(
            TestData.validIdentitetsnummer,
            TestData.validOrgNr,
            perioder
        )
        validate(krav) {
            validate(RefusjonskravDto::perioder).arbeidsgiverBetalerForDager(5, LocalDate.of(2021, 12, 1))
        }
    }
}
