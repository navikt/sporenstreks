package no.nav.helse.sporenstreks.domene

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDateTime
import java.util.*

@JsonIgnoreProperties("opprettetAv")
data class Refusjonskrav(
        val identitetsnummer: String,
        val virksomhetsnummer: String,
        val perioder: Set<Arbeidsgiverperiode>,

        var status: RefusjonskravStatus = RefusjonskravStatus.MOTTATT,
        var feilmelding: String? = null,
        var oppgaveId: String? = null,
        var joarkReferanse: String? = null,
        var kilde: String = "WEBSKJEMA",

        val opprettet: LocalDateTime = LocalDateTime.now(),
        val id: UUID = UUID.randomUUID(),
        var indeksertInflux: Boolean = false,

        // Dette referansenummeret overskrives av postgres ved lagring
        // og holdes utenfor JSON-data-feltet der. Det er kun skrivbart for mapping fra databasen
        var referansenummer: Int = 0
)

enum class RefusjonskravStatus {
    MOTTATT,
    SENDT_TIL_BEHANDLING,
    FEILET,
    AVBRUTT // Denne er ment som en måte å skru av prosesseringen for krav som skal ignoreres, men ikke skal slettes
}
