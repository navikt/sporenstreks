package no.nav.helse.sporenstreks.integrasjon

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OPPGAVETYPE_FORDELINGSOPPGAVE
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OppgaveKlient
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OpprettOppgaveRequest
import no.nav.helse.sporenstreks.domene.Refusjonskrav
import no.nav.helse.sporenstreks.domene.toRefusjonskravForOppgave
import java.time.LocalDate

class OppgaveService(private val oppgaveKlient: OppgaveKlient, private val om: ObjectMapper) {

    fun opprettOppgave(refusjonskrav: Refusjonskrav, journalpostId: String, aktørId: String, callId: String): String {
        val response = runBlocking {
            val request = mapOppgave(journalpostId, aktørId, mapStrukturert(refusjonskrav))
            oppgaveKlient.opprettOppgave(request, callId)
        }
        return "${response.id}"
    }

    fun opprettFordelingsOppgave(refusjonskrav: Refusjonskrav, aktørId: String, callId: String): String {
        val response = runBlocking {
            val fordelingsOppgaveReq = mapFordelingsOppgave(refusjonskrav, aktørId)
            oppgaveKlient.opprettOppgave(fordelingsOppgaveReq, callId)
        }
        return "${response.id}"
    }

    private fun mapStrukturert(refusjonskrav: Refusjonskrav): String {
        val kravForOppgave = refusjonskrav.toRefusjonskravForOppgave()
        return om.writeValueAsString(kravForOppgave)
    }

    private fun mapOppgave(journalpostId: String, aktørId: String, beskrivelse: String): OpprettOppgaveRequest {
        return OpprettOppgaveRequest(
                aktoerId = aktørId,
                journalpostId = journalpostId,
                beskrivelse = beskrivelse,
                tema = "SYK",
                oppgavetype = "ROB_BEH",
                behandlingstema = "ab0433",
                aktivDato = LocalDate.now(),
                fristFerdigstillelse = LocalDate.now().plusDays(7),
                prioritet = "NORM"
        )
    }
    private fun mapFordelingsOppgave(krav: Refusjonskrav, aktørId: String): OpprettOppgaveRequest {
        return OpprettOppgaveRequest(
                aktoerId = aktørId,
                journalpostId = krav.joarkReferanse,
                beskrivelse = "Klarte ikke journalføre og/eller opprette oppgave på denne personen.",
                tema = "SYK",
                oppgavetype = OPPGAVETYPE_FORDELINGSOPPGAVE,
                behandlingstema = "ab0433",
                aktivDato = LocalDate.now(),
                fristFerdigstillelse = LocalDate.now().plusDays(7),
                prioritet = "NORM"
        )
    }
}