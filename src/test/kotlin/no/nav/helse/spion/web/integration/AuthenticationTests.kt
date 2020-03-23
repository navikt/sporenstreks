package no.nav.helse.spion.web.integration

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.spion.web.dto.PersonOppslagDto
import no.nav.helse.validWithoutPeriode
import no.nav.security.token.support.test.JwtTokenGenerator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.koin.core.get

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