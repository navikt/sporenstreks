package no.nav.helse.sporenstreks.integrasjon.rest.oppgave

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
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
        private val url: String, private val stsClient: STSClient
) {

    private var httpClient = buildClient()

    private fun buildClient(): HttpClient {
        return HttpClient(Apache) {
            install(JsonFeature) {
                serializer = JacksonSerializer {
                    registerKotlinModule()
                    registerModule(JavaTimeModule())
                    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                }
                expectSuccess = false
            }
        }
    }

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
                aktivDato = LocalDate.now().plusDays(7),
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
            return OppgaveResultat(opprettOppgave(opprettOppgaveRequest, MDCOperations.generateCallId(), stsClient.getOidcToken()).id, false)
        } catch (ex : Exception) {
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

