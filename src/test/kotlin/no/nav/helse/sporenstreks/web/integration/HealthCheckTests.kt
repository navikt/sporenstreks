package no.nav.helse.sporenstreks.web.integration

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.server.testing.handleRequest
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.sporenstreks.auth.StaticMockAuthRepo
import no.nav.helse.sporenstreks.web.sporenstreksModule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.koin.ktor.ext.get

@KtorExperimentalAPI
class HealthCheckTests : ControllerIntegrationTestBase() {
    @KtorExperimentalLocationsAPI
    @Test
    fun `HealthCheck Endpoint returns 500 When any HealthCheck Component Fail`() {
        configuredTestApplication({
            sporenstreksModule()
            get<StaticMockAuthRepo>().failSelfCheck = true

        }) {
            handleRequest(HttpMethod.Get, "/healthcheck") {
            }.apply {
                assertEquals(HttpStatusCode.InternalServerError, response.status())
            }
        }
    }

    @KtorExperimentalLocationsAPI
    @Test
    fun `HealthCheck Endpoint returns 200 When all HealthCheck Components are Ok`() {
        configuredTestApplication({
            sporenstreksModule()
            get<StaticMockAuthRepo>().failSelfCheck = false

        }) {
            handleRequest(HttpMethod.Get, "/healthcheck") {
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
        }
    }


}