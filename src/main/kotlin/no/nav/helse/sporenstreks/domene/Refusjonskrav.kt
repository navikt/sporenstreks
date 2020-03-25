package no.nav.helse.sporenstreks.domene

import java.time.LocalDateTime
import java.util.*

data class Refusjonskrav(
    val opprettetAvIdentitetsnummer: String,
    val identitetsnummer: String,
    val virksomhetsnummer: String,
    val perioder: Set<Arbeidsgiverperiode>,
    val beløp: Double,
    val status: RefusjonskravStatus,
    val opprettet: LocalDateTime = LocalDateTime.now(),
    val id: UUID = UUID.randomUUID()
)

enum class RefusjonskravStatus {
    SENDT,
    FEILET
}
