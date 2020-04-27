package no.nav.helse.sporenstreks.integrasjon.rest

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.github.tomakehurst.wiremock.common.Json.toByteArray
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.http.ContentType
import io.ktor.http.headersOf
import io.mockk.mockk
import no.nav.helse.sporenstreks.domene.Refusjonskrav
import no.nav.helse.sporenstreks.integrasjon.rest.dokarkiv.DokarkivKlientImpl
import no.nav.helse.sporenstreks.integrasjon.rest.dokarkiv.JournalpostResponse
import no.nav.helse.sporenstreks.integrasjon.rest.sts.STSClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DokarkivKlientTest {


    val journalpostResponse = JournalpostResponse(
            journalpostId = "journalpostId",
            journalpostFerdigstilt = true,
            dokumenter = emptyList(),
            melding = null,
            journalStatus = "status"
    )
    val validResponse = journalpostResponse

    private val identitetsnummer = "01020354321"

    val stsMock = mockk<STSClient>(relaxed = true)


    val client = HttpClient(MockEngine) {

        install(JsonFeature) {
            serializer = JacksonSerializer {
                configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                registerModule(Jdk8Module())
                registerModule(KotlinModule())
                registerModule(JavaTimeModule())
                configure(SerializationFeature.INDENT_OUTPUT, true)
                configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
            }
        }

        engine {
            addHandler { request ->
                val url = request.url.toString()
                when {
                    url.startsWith("http://juice") -> {
                        val responseHeaders = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                        respond(toByteArray(validResponse), headers = responseHeaders)
                    }
                    else -> error("Unhandled ${request.url}")
                }
            }
        }
    }

    @Test
    internal fun `valid answer from altinn returns properly serialized list of all org forms`() {
        val dokarkivKlient = DokarkivKlientImpl("http://juice", client, stsMock)
        val refusjonskrav = Refusjonskrav(
                identitetsnummer = identitetsnummer,
                virksomhetsnummer = "123",
                perioder = emptySet(),
                joarkReferanse = "NUL",
                oppgaveId = "NULL"
        )
        val response = dokarkivKlient.journalførDokument("PDFDOKUMENT", refusjonskrav)
        assertThat(response).isNotNull()
    }


}

