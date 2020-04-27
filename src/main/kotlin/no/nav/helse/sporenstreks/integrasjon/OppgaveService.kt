package no.nav.helse.sporenstreks.integrasjon

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking
import no.nav.helse.sporenstreks.domene.Refusjonskrav
import no.nav.helse.sporenstreks.integrasjon.rest.oppgave.OppgaveKlient

class OppgaveService(private val oppgaveKlient: OppgaveKlient, private val om: ObjectMapper) {

    fun opprettOppgave(refusjonskrav: Refusjonskrav, journalpostId: String, aktørId: String, callId: String): String {
        val response = runBlocking {
            oppgaveKlient.opprettOppgave(
                    journalpostId = journalpostId,
                    aktørId = aktørId,
                    strukturertSkjema = mapStrukturert(refusjonskrav),
                    callId = callId
            )
        }
        return "${response.oppgaveId}"
    }

    fun mapStrukturert(refusjonskrav: Refusjonskrav): String {
        return om.writeValueAsString(refusjonskrav)
    }

}