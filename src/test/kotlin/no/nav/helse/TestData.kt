package no.nav.helse

import no.nav.helse.spion.domene.Periode
import no.nav.helse.spion.web.dto.PersonOppslagDto
import java.time.LocalDate

object TestData {
    val validIdentitetsnummer = "20015001543"
    val notValidIdentitetsnummer = "50012001987"
    val validOrgNr = "123456785"
    val notValidOrgNr = "123456789"
}


fun PersonOppslagDto.Companion.validWithoutPeriode() = PersonOppslagDto(TestData.validIdentitetsnummer, TestData.validOrgNr)
fun PersonOppslagDto.Companion.validWithPeriode(fom: LocalDate, tom: LocalDate) = PersonOppslagDto(TestData.validIdentitetsnummer, TestData.validOrgNr, Periode(fom, tom))
