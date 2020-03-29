package no.nav.helse.sporenstreks.integrasjon.rest.oppgave

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.helse.sporenstreks.integrasjon.rest.sts.STSClient
import no.nav.helse.sporenstreks.utils.MDCOperations
import org.slf4j.LoggerFactory
import java.time.LocalDate

interface OppgaveKlient {
    suspend fun opprettOppgave(
            journalpostId: String,
            aktørId: String,
            strukturertSkjema: String
    ): OppgaveResultat
}

class MockOppgaveKlient : OppgaveKlient {
    override suspend fun opprettOppgave(journalpostId: String, aktørId: String, strukturertSkjema: String): OppgaveResultat {
        return OppgaveResultat(123, false)
    }
}

class OppgaveKlientImpl(
        private val url: String, private val stsClient: STSClient, private val httpClient: HttpClient
) : OppgaveKlient {


    private suspend fun opprettOppgave(opprettOppgaveRequest: OpprettOppgaveRequest, msgId: String, token: String): OpprettOppgaveResponse {
        return httpClient.post(url) {
            contentType(ContentType.Application.Json)
            this.header("Authorization", "Bearer $token")
            this.header("X-Correlation-ID", msgId)
            body = opprettOppgaveRequest
        }
    }

    fun mapOppgave(journalpostId: String, aktørId: String, beskrivelse: String): OpprettOppgaveRequest {
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

    override suspend fun opprettOppgave(
            journalpostId: String,
            aktørId: String,
            strukturertSkjema: String
    ): OppgaveResultat {
        val opprettOppgaveRequest = mapOppgave(journalpostId, aktørId, strukturertSkjema)
        log.info("Oppretter oppgave")
        return OppgaveResultat(
                opprettOppgave(
                        opprettOppgaveRequest,
                        MDCOperations.generateCallId(), // TODO Må gjenbruke callId fra første kall i kjeden
                        stsClient.getOidcToken()
                ).id, false
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(STSClient::class.java)
    }

}

data class OpprettOppgaveRequest(
        val tildeltEnhetsnr: String? = null,
        val aktoerId: String? = null,
        val journalpostId: String? = null,
        val behandlesAvApplikasjon: String? = null,
        val saksreferanse: String? = null,
        val beskrivelse: String? = null,
        val tema: String? = null,
        val oppgavetype: String,
        val behandlingstype: String? = null,
        val behandlingstema: String? = null,
        val aktivDato: LocalDate,
        val fristFerdigstillelse: LocalDate? = null,
        val prioritet: String
)

data class OpprettOppgaveResponse(
        val id: Int
)

data class OppgaveResultat(
        val oppgaveId: Int,
        val duplikat: Boolean
)

