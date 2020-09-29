package no.nav.helse.sporenstreks.web.integration

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.server.testing.handleRequest
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.arbeidsgiver.kubernetes.KubernetesProbeManager
import no.nav.helse.arbeidsgiver.kubernetes.LivenessComponent
import no.nav.helse.arbeidsgiver.kubernetes.ReadynessComponent
import no.nav.helse.sporenstreks.auth.StaticMockAuthRepo
import no.nav.helse.sporenstreks.web.getAllOfType
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
            val probeManager = application.get<KubernetesProbeManager>()
            probeManager.registerReadynessComponent(application.get<StaticMockAuthRepo>())
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
            val probeManager = application.get<KubernetesProbeManager>()
            probeManager.registerReadynessComponent(StaticMockAuthRepo(application.get<ObjectMapper>()))
            handleRequest(HttpMethod.Get, "/healthcheck") {
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
        }
    }


}