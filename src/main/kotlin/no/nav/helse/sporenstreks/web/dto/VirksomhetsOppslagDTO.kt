package no.nav.helse.sporenstreks.web.dto

import io.ktor.locations.Location
import no.nav.helse.sporenstreks.web.dto.validation.isValidOrganisasjonsnummer
import org.valiktor.validate

@Location("/virksomhet/{virksomhetsnummer}")
data class VirksomhetsOppslagDTO(val virksomhetsnummer: String) {

    init {
        validate(this) {
            validate(VirksomhetsOppslagDTO::virksomhetsnummer).isValidOrganisasjonsnummer()
        }
    }
}
