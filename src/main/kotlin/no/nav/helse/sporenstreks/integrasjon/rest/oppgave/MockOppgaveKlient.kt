package no.nav.helse.sporenstreks.integrasjon.rest.oppgave

import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.*
import java.time.LocalDateTime.now

class MockOppgaveKlient : OppgaveKlient {
    override suspend fun opprettOppgave(
        opprettOppgaveRequest: OpprettOppgaveRequest,
        callId: String
    ): OpprettOppgaveResponse {
        return OpprettOppgaveResponse(
            1234,
            "2",
            "SYK",
            "ROB",
            1,
            now().toLocalDate(),
            Prioritet.NORM,
            Status.OPPRETTET
        )
    }
}
