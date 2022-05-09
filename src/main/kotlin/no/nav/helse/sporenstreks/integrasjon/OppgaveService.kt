package no.nav.helse.sporenstreks.integrasjon

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OppgaveKlient
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OpprettOppgaveRequest
import no.nav.helse.sporenstreks.domene.Refusjonskrav
import no.nav.helse.sporenstreks.domene.toRefusjonskravForOppgave
import java.time.LocalDate

class OppgaveService(private val oppgaveKlient: OppgaveKlient, private val om: ObjectMapper) {

    fun opprettOppgave(refusjonskrav: Refusjonskrav, journalpostId: String, aktørId: String, callId: String): String {
        val response = runBlocking {
            val request = mapOppgave(journalpostId, aktørId, mapStrukturert(refusjonskrav), refusjonskrav.tariffEndring)
            oppgaveKlient.opprettOppgave(request, callId)
        }
        return "${response.id}"
    }

    private fun mapStrukturert(refusjonskrav: Refusjonskrav): String {
        val kravForOppgave = refusjonskrav.toRefusjonskravForOppgave()
        return om.writeValueAsString(kravForOppgave)
    }

    private fun mapOppgave(journalpostId: String, aktørId: String, beskrivelse: String, tariffEndring: Boolean): OpprettOppgaveRequest {
        val oppgaveType = if (!tariffEndring) "ROB_BEH" else "VUR_KONS_YTE"
        val behandlingstype = if (!tariffEndring) "ae0052" else null
        val behandlingstema = "ab0456"

        return OpprettOppgaveRequest(
            aktoerId = aktørId,
            journalpostId = journalpostId,
            beskrivelse = beskrivelse,
            tema = "SYK",
            oppgavetype = oppgaveType,
            behandlingstema = behandlingstema,
            aktivDato = LocalDate.now(),
            fristFerdigstillelse = LocalDate.now().plusDays(7),
            prioritet = "NORM",
            behandlingstype = behandlingstype,
        )
    }
}
