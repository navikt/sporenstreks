package no.nav.helse.sporenstreks.domene

import java.time.LocalDateTime
import java.util.*

data class Refusjonskrav(
        val opprettetAv: String,
        val identitetsnummer: String,
        val virksomhetsnummer: String,
        val perioder: Set<Arbeidsgiverperiode>,

        var status: RefusjonskravStatus = RefusjonskravStatus.MOTTATT,
        var oppgaveId: String? = null,
        var joarkReferanse: String? = null,

        val opprettet: LocalDateTime = LocalDateTime.now(),
        val id: UUID = UUID.randomUUID(),

        // Dette referansenummeret overskrives av postgres ved lagring
        // og holdes utenfor JSON-data-feltet der. Det er kun skrivbart for mapping fra databasen
        var referansenummer: Int = 0
)

enum class RefusjonskravStatus {
    MOTTATT,
    SENDT_TIL_BEHANDLING,
    FEILET
}