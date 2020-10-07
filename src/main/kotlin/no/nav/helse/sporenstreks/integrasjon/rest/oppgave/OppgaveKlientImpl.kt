package no.nav.helse.sporenstreks.integrasjon.rest.oppgave

import io.ktor.client.HttpClient
import io.ktor.client.features.*
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.utils.io.*
import no.nav.helse.arbeidsgiver.integrasjoner.RestStsClient
import org.slf4j.LoggerFactory
import java.time.LocalDate

interface OppgaveKlient {
    suspend fun opprettOppgave(
            journalpostId: String,
            aktørId: String,
            strukturertSkjema: String,
            callId: String
    ): OppgaveResultat
}

class MockOppgaveKlient : OppgaveKlient {
    override suspend fun opprettOppgave(journalpostId: String, aktørId: String, strukturertSkjema: String, callId: String): OppgaveResultat {
        return OppgaveResultat(123, false)
    }
}

class OppgaveKlientImpl(
        private val url: String, private val stsClient: RestStsClient, private val httpClient: HttpClient
) : OppgaveKlient {


    private suspend fun opprettOppgave(opprettOppgaveRequest: OpprettOppgaveRequest, msgId: String, token: String): OpprettOppgaveResponse {
        try {
            return httpClient.post(url) {
                contentType(ContentType.Application.Json)
                this.header("Authorization", "Bearer $token")
                this.header("X-Correlation-ID", msgId)
                body = opprettOppgaveRequest
            }
        } catch (ex: ResponseException) {
            log.error(ex.response.content.readUTF8Line())
            throw ex
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
            strukturertSkjema: String,
            callId: String
    ): OppgaveResultat {
        val opprettOppgaveRequest = mapOppgave(journalpostId, aktørId, strukturertSkjema)
        log.debug("Oppretter oppgave")
        return OppgaveResultat(
                opprettOppgave(
                        opprettOppgaveRequest,
                        callId,
                        stsClient.getOidcToken()
                ).id, false
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(OppgaveKlientImpl::class.java)
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

