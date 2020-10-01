package no.nav.helse.sporenstreks.auth.altinn

import com.fasterxml.jackson.databind.MapperFeature
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.features.ServerResponseException
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import loadFromResources
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class AltinnClientTests {

    val validAltinnResponse = "mock-data/altinn/organisasjoner-med-rettighet.json".loadFromResources()

    private val identitetsnummer = "01020354321"
    private val serviceCode = "4444"

    val client = HttpClient(MockEngine) {

        install(JsonFeature) {
            serializer = JacksonSerializer {
                configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
            }
        }

        engine {
            addHandler { request ->
                val url = request.url.toString()
                when {
                    url.startsWith("http://juice") -> {
                        val responseHeaders = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                        respond(validAltinnResponse, headers = responseHeaders)
                    }
                    url.startsWith("http://timeout") -> {
                        respond("Timed out", HttpStatusCode.GatewayTimeout)
                    }
                    url.startsWith("http://altinn-timeout") -> {
                        respond("Timed out", HttpStatusCode.BadGateway)
                    }
                    else -> error("Unhandled ${request.url}")
                }
            }
        }
    }

    @Test
    internal fun `valid answer from altinn returns properly serialized list of all active org forms`() {
        val altinnClient = AltinnClient("http://juice", "api-gw-key", "altinn-key", serviceCode, client)
        val authList = altinnClient.hentOrgMedRettigheterForPerson(identitetsnummer)
        assertThat(authList).hasSize(4)
    }


    @Test
    internal fun `timeout from altinn throws exception`() {
        val altinnClient = AltinnClient("http://timeout", "api-gw-key", "altinn-key", serviceCode, client)

        assertThrows(ServerResponseException::class.java) {
            altinnClient.hentOrgMedRettigheterForPerson(identitetsnummer)
        }
    }

    //@Test
    internal fun `timeout from altinn fails the health check`() {
        val altinnClient = AltinnClient("http://timeout", "api-gw-key", "altinn-key", serviceCode, client)

        assertThrows(ServerResponseException::class.java) {
            runBlocking { altinnClient.runReadynessCheck() }
        }
    }

    @Test
    internal fun `timeout from altinn throws AltinnBrukteForLangTidException`() {
        val altinnClient = AltinnClient("http://altinn-timeout", "api-gw-key", "altinn-key", serviceCode, client)
        assertThrows(AltinnBrukteForLangTidException::class.java) {
            runBlocking { altinnClient.hentOrgMedRettigheterForPerson(identitetsnummer) }
        }
    }

    @Test
    internal suspend fun `readiness check passes with valid response from altinn`() {
        val altinnClient = AltinnClient("http://juice", "api-gw-key", "altinn-key", serviceCode, client)
        altinnClient.runReadynessCheck()
    }
}