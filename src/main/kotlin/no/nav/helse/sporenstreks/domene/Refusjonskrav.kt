package no.nav.helse.sporenstreks.domene

import java.time.LocalDateTime
import java.util.*

data class Refusjonskrav(
        var opprettetAv: String,
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
        var kvitteringId: UUID? = null,
        var indeksertInflux: Boolean = false,

        // Dette referansenummeret overskrives av postgres ved lagring
        // og holdes utenfor JSON-data-feltet der. Det er kun skrivbart for mapping fra databasen
        var referansenummer: Int = 0
): Comparable<Refusjonskrav> {
    override fun compareTo(other: Refusjonskrav): Int {
        if (other.identitetsnummer > identitetsnummer)
            return -1
        if (identitetsnummer > other.identitetsnummer)
            return 1
        if (other.perioder.first().fom.isAfter(perioder.first().fom))
            return -1
        if (other.perioder.first().fom.isBefore(perioder.first().fom))
            return 1
        if (other.opprettet.isAfter(opprettet))
            return -1
        if (other.opprettet.isBefore(opprettet))
            return 1
        return 0
    }
}

enum class RefusjonskravStatus {
    MOTTATT,
    SENDT_TIL_BEHANDLING,
    FEILET,
    JOBB,
    AVBRUTT // Denne er ment som en måte å skru av prosesseringen for krav som skal ignoreres, men ikke skal slettes
}
