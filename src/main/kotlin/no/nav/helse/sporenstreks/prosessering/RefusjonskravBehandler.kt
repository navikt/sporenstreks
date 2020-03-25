package no.nav.helse.sporenstreks.prosessering

import no.nav.helse.sporenstreks.domene.Refusjonskrav
import no.nav.helse.sporenstreks.integrasjon.JoarkService
import no.nav.helse.sporenstreks.integrasjon.OppgaveService

class RefusjonskravBehandler(val joarkService: JoarkService,
                             val oppgaveService: OppgaveService) {

    fun behandle(refusjonskrav: Refusjonskrav) {
        val journalpostId = joarkService.journalfør(refusjonskrav)
        oppgaveService.opprettManuellJournalføringsoppgave(refusjonskrav, journalpostId)
    }


}