package no.nav.helse.sporenstreks.integrasjon.rest.oppgave

import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OppgaveKlient
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OpprettOppgaveRequest
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OpprettOppgaveResponse

class MockOppgaveKlient : OppgaveKlient {
    override suspend fun opprettOppgave(opprettOppgaveRequest: OpprettOppgaveRequest, callId: String): OpprettOppgaveResponse {
        return OpprettOppgaveResponse(1234)
    }
}
