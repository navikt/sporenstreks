package no.nav.helse.spion.web.dto

import no.nav.helse.TestData
import no.nav.helse.validWithPeriode
import no.nav.helse.validWithoutPeriode
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.valiktor.ConstraintViolationException
import java.time.LocalDate

internal class PersonOppslagDtoTest {

    @Test
    fun `Ugyldig identitetsnummer gir valideringsfeil`() {
        Assertions.assertThatExceptionOfType(ConstraintViolationException::class.java).isThrownBy {
            PersonOppslagDto(TestData.notValidIdentitetsnummer, TestData.validOrgNr)
        }
    }

    @Test
    fun `Ugyldig org-nummer gir valideringsfeil`() {
        Assertions.assertThatExceptionOfType(ConstraintViolationException::class.java).isThrownBy {
            PersonOppslagDto(TestData.validIdentitetsnummer, TestData.notValidOrgNr)
        }
    }

    @Test
    fun `Gyldig idnr og arbeidsgiverId og tom etter fom gir ingen feil`() {
        PersonOppslagDto.validWithPeriode(LocalDate.MIN, LocalDate.now())
    }

    @Test
    fun `Gyldig idnr og arbeidsgiverId uten periode gir ingen feil`() {
        PersonOppslagDto.validWithoutPeriode()
    }

    @Test
    fun `Om det finnes en periode må tom være etter fom`() {
        Assertions.assertThatExceptionOfType(ConstraintViolationException::class.java).isThrownBy {
            PersonOppslagDto.validWithPeriode(LocalDate.MAX, LocalDate.now())
        }
    }
}