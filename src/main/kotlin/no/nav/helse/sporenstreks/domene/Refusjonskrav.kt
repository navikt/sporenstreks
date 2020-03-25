package no.nav.helse.sporenstreks.domene

import no.nav.helse.sporenstreks.web.dto.RefusjonskravDto
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

fun fraDto(dto: RefusjonskravDto): Refusjonskrav {
    return Refusjonskrav(
            opprettetAvIdentitetsnummer = "TODO",
            identitetsnummer = dto.identitetsnummer,
            virksomhetsnummer = dto.virksomhetsnummer,
            perioder = dto.perioder,
            beløp = dto.beløp,
            status = RefusjonskravStatus.SENDT
    )
}

enum class RefusjonskravStatus {
    SENDT,
    FEILET
}
