package no.nav.helse.sporenstreks.domene
import java.time.LocalDateTime
import java.util.*

data class RefusjonskravForOppgave(
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

    val soekerForSegSelv: Boolean,

    // Dette referansenummeret overskrives av postgres ved lagring
    // og holdes utenfor JSON-data-feltet der. Det er kun skrivbart for mapping fra databasen
    var referansenummer: Int = 0
)
fun Refusjonskrav.toRefusjonskravForOppgave() = RefusjonskravForOppgave(
    identitetsnummer = identitetsnummer,
    virksomhetsnummer = virksomhetsnummer,
    perioder = perioder,
    status = status,
    feilmelding = feilmelding,
    oppgaveId = oppgaveId,
    joarkReferanse = joarkReferanse,
    kilde = kilde,
    opprettet = opprettet,
    id = id,
    kvitteringId = kvitteringId,
    indeksertInflux = indeksertInflux,
    referansenummer = referansenummer,
    soekerForSegSelv = identitetsnummer == opprettetAv
)
