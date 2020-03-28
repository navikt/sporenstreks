package no.nav.helse.sporenstreks.prosessering

import no.nav.helse.sporenstreks.db.PostgresRefusjonskravRepository
import no.nav.helse.sporenstreks.domene.Refusjonskrav
import no.nav.helse.sporenstreks.domene.RefusjonskravStatus
import no.nav.helse.sporenstreks.integrasjon.JoarkService
import no.nav.helse.sporenstreks.integrasjon.OppgaveService
import no.nav.helse.sporenstreks.metrics.FEIL_COUNTER

class RefusjonskravBehandler(val joarkService: JoarkService,
                             val oppgaveService: OppgaveService,
                             val repository: PostgresRefusjonskravRepository) {

    fun behandle(refusjonskrav: Refusjonskrav) {
        try {
            if (refusjonskrav.joarkReferanse.isNullOrBlank()) {
                refusjonskrav.joarkReferanse = joarkService.journalfør(refusjonskrav)
                repository.update(refusjonskrav) // Unødvendig å lagre i mellom her kanskje sålenge vi updater i catchen?
            }
            if (refusjonskrav.oppgaveId.isNullOrBlank()) {
                refusjonskrav.oppgaveId = oppgaveService.opprettOppgave(refusjonskrav, refusjonskrav.joarkReferanse!!)
                repository.update(refusjonskrav)
            }
            refusjonskrav.status = RefusjonskravStatus.SENDT_TIL_BEHANDLING
            repository.update(refusjonskrav)
        } catch (e: Exception) {
            refusjonskrav.status = RefusjonskravStatus.FEILET
            refusjonskrav.feilmelding = e.cause.toString() //TODO Finpuss
            FEIL_COUNTER.inc()
            repository.update(refusjonskrav) // Hva gjør vi om dette kallet feiler? Printe til loggene?
        }
    }
}