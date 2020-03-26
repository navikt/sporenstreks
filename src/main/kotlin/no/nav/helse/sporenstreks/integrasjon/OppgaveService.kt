package no.nav.helse.sporenstreks.integrasjon

import no.nav.helse.sporenstreks.domene.Refusjonskrav
import no.nav.helse.sporenstreks.integrasjon.rest.oppgave.OppgaveKlient

class OppgaveService(val oppgaveKlient: OppgaveKlient) {

    fun opprettManuellJournalføringsoppgave(refusjonskrav: Refusjonskrav, journalpostId: String) {
    }

}