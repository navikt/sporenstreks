package no.nav.helse.sporenstreks.web.integration

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.sporenstreks.web.dto.PersonOppslagDto
import no.nav.helse.sporenstreks.web.sporenstreksModule
import no.nav.helse.validWithoutPeriode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@KtorExperimentalAPI
class ApplicationAuthenticationTest : ControllerIntegrationTestBase() {

    val oppslag = PersonOppslagDto.validWithoutPeriode()


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