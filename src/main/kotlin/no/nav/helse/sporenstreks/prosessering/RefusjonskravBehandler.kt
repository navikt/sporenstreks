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
                //repository.update(refusjonskrav) TODO
            }
            if (refusjonskrav.oppgaveId.isNullOrBlank()) {
                refusjonskrav.oppgaveId = oppgaveService.opprettOppgave(refusjonskrav, "123", "123", "123") // TODO
                //repository.update(refusjonskrav) TODO
            }
            refusjonskrav.status = RefusjonskravStatus.SENDT_TIL_BEHANDLING
            //repository.update(refusjonskrav) TODO
        } catch (e: Exception) {
            refusjonskrav.status = RefusjonskravStatus.FEILET
            refusjonskrav.feilmelding = e.cause.toString() //TODO Finpuss
            FEIL_COUNTER.inc()
            //repository.update TODO
        }
    }


}