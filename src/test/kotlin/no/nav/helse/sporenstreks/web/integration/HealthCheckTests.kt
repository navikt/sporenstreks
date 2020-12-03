package no.nav.helse.sporenstreks.web.integration

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.*
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.arbeidsgiver.kubernetes.KubernetesProbeManager
import no.nav.helse.sporenstreks.auth.StaticMockAuthRepo
import no.nav.helse.sporenstreks.web.sporenstreksModule
import org.junit.jupiter.api.Test
import org.koin.ktor.ext.get


@KtorExperimentalAPI
class HealthCheckTests : ControllerIntegrationTestBase() {
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
                kotlin.test.assertEquals(HttpStatusCode.InternalServerError, response.status())
            }
        }
    }

    @Test
    fun `HealthCheck Endpoint returns 200 When all HealthCheck Components are Ok`() {
        configuredTestApplication({
            sporenstreksModule()
            get<StaticMockAuthRepo>().failSelfCheck = false

        }) {
            val probeManager = application.get<KubernetesProbeManager>()
            probeManager.registerReadynessComponent(application.get<StaticMockAuthRepo>())
            handleRequest(HttpMethod.Get, "/healthcheck") {
            }.apply {
                kotlin.test.assertEquals(HttpStatusCode.OK, response.status())
            }
        }
    }
}