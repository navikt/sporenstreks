package no.nav.helse.sporenstreks.web.integration

import io.ktor.http.*
import io.ktor.locations.*
import io.ktor.server.testing.*
import io.ktor.util.*
import no.nav.helse.sporenstreks.web.sporenstreksModule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test


@KtorExperimentalAPI
class ApplicationAuthenticationTest : ControllerIntegrationTestBase() {

    @KtorExperimentalLocationsAPI
    @Test
    fun `nais isalive endpoint with no JWT returns 200 OK`() {
        configuredTestApplication({
            sporenstreksModule()
        }) {
            handleRequest(HttpMethod.Get, "/isalive") {
            }.apply {
                assertThat(response.status()).isEqualTo(HttpStatusCode.OK)
            }
        }
    }

    @KtorExperimentalLocationsAPI
    @Test
    fun `nais isready endpoint with no JWT returns 200 OK`() {
        configuredTestApplication({
            sporenstreksModule()
        }) {
            handleRequest(HttpMethod.Get, "/isready") {
            }.apply {
                assertThat(response.status()).isEqualTo(HttpStatusCode.OK)
            }
        }
    }
}