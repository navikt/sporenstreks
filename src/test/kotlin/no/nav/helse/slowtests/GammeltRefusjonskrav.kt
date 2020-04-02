package no.nav.helse.slowtests

import no.nav.helse.sporenstreks.domene.Arbeidsgiverperiode
import java.time.LocalDateTime
import java.util.*

data class GammeltRefusjonskrav(
        val opprettetAv: String,
        val identitetsnummer: String,
        val virksomhetsnummer: String,
        val perioder: Set<Arbeidsgiverperiode>,

        var status: GammeltRefusjonskravStatus = GammeltRefusjonskravStatus.MOTTATT,
        var feilmelding: String? = null,
        var oppgaveId: String? = null,
        var joarkReferanse: String? = null,

        val opprettet: LocalDateTime = LocalDateTime.now(),
        val id: UUID = UUID.randomUUID(),

        // Dette referansenummeret overskrives av postgres ved lagring
        // og holdes utenfor JSON-data-feltet der. Det er kun skrivbart for mapping fra databasen
        var referansenummer: Int = 0
)

enum class GammeltRefusjonskravStatus {
    MOTTATT,
    SENDT_TIL_BEHANDLING,
    FEILET,
    AVBRUTT
}