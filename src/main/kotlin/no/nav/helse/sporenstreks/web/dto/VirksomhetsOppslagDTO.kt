package no.nav.helse.spion.web.dto

import io.ktor.locations.Location
import no.nav.helse.spion.domene.Periode
import org.valiktor.functions.isGreaterThanOrEqualTo
import org.valiktor.functions.validate
import org.valiktor.validate
import java.time.LocalDate

@Location("/virksomhet/{virksomhetsnummer}")
data class VirksomhetsOppslagDTO(val virksomhetsnummer: String, val fom: LocalDate, val tom: LocalDate) {
    val periode = Periode(fom, tom)

    init {
        validate(this) {
            validate(VirksomhetsOppslagDTO::virksomhetsnummer).isValidOrganisasjonsnummer()

            validate(VirksomhetsOppslagDTO::periode).validate {
                validate(Periode::tom).isGreaterThanOrEqualTo(it.fom)
            }
        }
    }
}