package no.nav.helse.sporenstreks.integrasjon

import com.fasterxml.jackson.databind.ObjectMapper
import com.sun.tools.javac.code.Kinds.KindSelector.VAL
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
        val oppgaveType = if (tariffEndring) "REF_BEH" else "ROB_BEH"
        val behandlingsTema = if (tariffEndring) "ab0433" else "XXX"

        return OpprettOppgaveRequest(
            aktoerId = aktørId,
            journalpostId = journalpostId,
            beskrivelse = beskrivelse,
            tema = "SYK",
            oppgavetype = oppgaveType,
            behandlingstema = behandlingsTema,
            aktivDato = LocalDate.now(),
            fristFerdigstillelse = LocalDate.now().plusDays(7),
            prioritet = "NORM"
        )
    }
}
