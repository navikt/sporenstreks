package no.nav.helse.sporenstreks.web.dto

import no.nav.helse.sporenstreks.domene.Periode
import no.nav.helse.sporenstreks.web.dto.validation.isValidIdentitetsnummer
import no.nav.helse.sporenstreks.web.dto.validation.isValidOrganisasjonsnummer
import org.valiktor.functions.isGreaterThanOrEqualTo
import org.valiktor.functions.validate
import org.valiktor.validate

class PersonOppslagDto(
        val identitetsnummer: String,
        val arbeidsgiverId: String,
        val periode: Periode? = null
) {
    companion object;

    init {
        validate(this) {
            validate(PersonOppslagDto::identitetsnummer).isValidIdentitetsnummer()
            validate(PersonOppslagDto::arbeidsgiverId).isValidOrganisasjonsnummer()

            validate(PersonOppslagDto::periode).validate {
                validate(Periode::tom).isGreaterThanOrEqualTo(it.fom)
            }
        }
    }

}