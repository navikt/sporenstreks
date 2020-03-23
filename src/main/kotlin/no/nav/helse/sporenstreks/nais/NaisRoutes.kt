package no.nav.helse.spion.nais

import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.response.respondTextWriter
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.util.pipeline.PipelineContext
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import io.prometheus.client.hotspot.DefaultExports
import no.nav.helse.spion.selfcheck.HealthCheck
import no.nav.helse.spion.selfcheck.HealthCheckState
import no.nav.helse.spion.selfcheck.HealthCheckType
import no.nav.helse.spion.selfcheck.runHealthChecks
import org.koin.ktor.ext.get
import org.koin.ktor.ext.getKoin
import java.time.LocalDate
import java.util.*

private val collectorRegistry = CollectorRegistry.defaultRegistry

fun Application.nais() {

    DefaultExports.initialize()

    routing {
        get("/isalive") {
            returnResultOfChecks(this@routing, HealthCheckType.ALIVENESS, this)
        }

        get("/isready") {
            returnResultOfChecks(this@routing, HealthCheckType.READYNESS, this)
        }

        get("/metrics") {
            val names = call.request.queryParameters.getAll("name[]")?.toSet() ?: Collections.emptySet()
            call.respondTextWriter(ContentType.parse(TextFormat.CONTENT_TYPE_004)) {
                TextFormat.write004(this, collectorRegistry.filteredMetricFamilySamples(names))
            }
        }

        get("/healthcheck") {
            val allRegisteredSelfCheckComponents = this@routing.getKoin().getAllOfType<HealthCheck>()
            val checkResults = runHealthChecks(allRegisteredSelfCheckComponents)
            val httpResult = if (checkResults.any { it.state == HealthCheckState.ERROR }) HttpStatusCode.InternalServerError else HttpStatusCode.OK

            call.respond(httpResult, checkResults)
        }

    }
}

private suspend fun returnResultOfChecks(routing: Routing, type: HealthCheckType, pipelineContext: PipelineContext<Unit, ApplicationCall>) {
    val allRegisteredSelfCheckComponents = routing.getKoin()
            .getAllOfType<HealthCheck>()
            .filter { it.healthCheckType == type }

    val checkResults = runHealthChecks(allRegisteredSelfCheckComponents)
    val httpResult = if (checkResults.any { it.state == HealthCheckState.ERROR }) HttpStatusCode.InternalServerError else HttpStatusCode.OK
    checkResults.forEach { r ->
        r.error?.let { pipelineContext.call.application.environment.log.error(r.toString()) }
    }
    pipelineContext.call.respond(httpResult, checkResults)
}


