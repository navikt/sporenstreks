package no.nav.helse.sporenstreks.nais

import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.response.respondTextWriter
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.util.pipeline.PipelineContext
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import io.prometheus.client.hotspot.DefaultExports
import no.nav.helse.arbeidsgiver.kubernetes.KubernetesProbeManager
import no.nav.helse.arbeidsgiver.kubernetes.ProbeResult
import no.nav.helse.arbeidsgiver.kubernetes.ProbeState
import org.koin.ktor.ext.get
import org.slf4j.LoggerFactory
import java.util.*

private val collectorRegistry = CollectorRegistry.defaultRegistry

fun Application.nais() {

    val log = LoggerFactory.getLogger("Metrics Routes")
    DefaultExports.initialize()

    routing {
        get("/isalive") {
            val kubernetesProbeManager = this@routing.get<KubernetesProbeManager>()
            val checkResults = kubernetesProbeManager.runLivenessProbe()
            log.debug(checkResults.toString())
            returnResultOfChecks(checkResults)
        }

        get("/isready") {
            val kubernetesProbeManager = this@routing.get<KubernetesProbeManager>()
            val checkResults = kubernetesProbeManager.runReadynessProbe()
            log.debug(checkResults.toString())
            returnResultOfChecks( checkResults)
        }

        get("/metrics") {
            val names = call.request.queryParameters.getAll("name[]")?.toSet() ?: Collections.emptySet()
            call.respondTextWriter(ContentType.parse(TextFormat.CONTENT_TYPE_004)) {
                TextFormat.write004(this, collectorRegistry.filteredMetricFamilySamples(names))
            }
        }

        get("/healthcheck") {
            val kubernetesProbeManager = this@routing.get<KubernetesProbeManager>()
            val readyResults = kubernetesProbeManager.runReadynessProbe()
            val liveResults = kubernetesProbeManager.runLivenessProbe()
            val combinedResults = ProbeResult(
                    liveResults.healthyComponents +
                            liveResults.unhealthyComponents +
                            readyResults.healthyComponents +
                            readyResults.unhealthyComponents
            )

            returnResultOfChecks(combinedResults)
        }
    }
}

private suspend fun PipelineContext<Unit, ApplicationCall>.returnResultOfChecks(checkResults: ProbeResult) {
    val httpResult = if (checkResults.state == ProbeState.UN_HEALTHY) HttpStatusCode.InternalServerError else HttpStatusCode.OK
    checkResults.unhealthyComponents.forEach { r ->
        r.error?.let { call.application.environment.log.error(r.toString()) }
    }
    call.respond(httpResult, checkResults)
}


