package no.nav.helse.sporenstreks.domene

import java.time.LocalDateTime
import java.util.*

data class Refusjonskrav(
    val opprettetAv: String,
    val identitetsnummer: String,
    val virksomhetsnummer: String,
    val perioder: Set<Arbeidsgiverperiode>,
    val beløp: Double,

    val joarkReferanse: String,
    val oppgaveId: String,

    val opprettet: LocalDateTime = LocalDateTime.now(),
    val id: UUID = UUID.randomUUID()
)