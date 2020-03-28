package no.nav.helse.sporenstreks.integrasjon.rest.oppgave

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.helse.sporenstreks.integrasjon.rest.sts.STSClient
import no.nav.helse.sporenstreks.utils.MDCOperations
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class OppgaveKlient constructor (
        private val url: String, private val stsClient: STSClient, private val httpClient: HttpClient
) {


    private suspend fun opprettOppgave(opprettOppgaveRequest: OpprettOppgaveRequest, msgId: String, token: String): OpprettOppgaveResponse {
        return httpClient.post(url) {
            contentType(ContentType.Application.Json)
            this.header("Authorization", "Bearer $token")
            this.header("X-Correlation-ID", msgId)
            body = opprettOppgaveRequest
        }
    }

    fun mapOppgave(sakId: String, journalpostId: String, aktørId: String, beskrivelse: String): OpprettOppgaveRequest {
        return OpprettOppgaveRequest(
                aktoerId = aktørId,
                journalpostId = journalpostId,
                saksreferanse = sakId,
                beskrivelse = beskrivelse,
                tema = "SYK",
                oppgavetype = "Robotbehandling",
                behandlingstema = "refusjonskrav dag 4",
                aktivDato = LocalDate.now(),
                fristFerdigstillelse= LocalDate.now().plusDays(7),
                prioritet = "NORM"
        )
    }

    suspend fun opprettOppgave(
            sakId: String,
            journalpostId: String,
            aktørId: String,
            strukturertSkjema: String
    ): OppgaveResultat {
        val opprettOppgaveRequest = mapOppgave(sakId, journalpostId, aktørId, strukturertSkjema)
        try {
            log.info("Oppretter oppgave")
            return OppgaveResultat(
                    opprettOppgave(
                            opprettOppgaveRequest,
                            MDCOperations.generateCallId(), // TODO Må gjenbruke callId fra første kall i kjeden
                            stsClient.getOidcToken()
                    ).id, false
            )
        } catch (ex: Exception) {
            throw OpprettOppgaveException(journalpostId)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(STSClient::class.java)
    }

}

class OpprettOppgaveException(message: String) : Exception(message)

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

