package no.nav.helse.sporenstreks.integrasjon

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking
import no.nav.helse.sporenstreks.domene.Refusjonskrav
import no.nav.helse.sporenstreks.integrasjon.rest.oppgave.OppgaveKlient

class OppgaveService(private val oppgaveKlient: OppgaveKlient, private val om: ObjectMapper) {

    fun opprettOppgave(refusjonskrav: Refusjonskrav, journalpostId: String): String {
        val response = runBlocking {
            oppgaveKlient.opprettOppgave(
                    sakId = "123",
                    journalpostId = journalpostId,
                    aktørId = "123",
                    strukturertSkjema = mapStrukturert(refusjonskrav)
            )
        }
        return "${response.oppgaveId}"
    }

    fun mapStrukturert(refusjonskrav: Refusjonskrav): String {
        return om.writeValueAsString(refusjonskrav)
    }

}