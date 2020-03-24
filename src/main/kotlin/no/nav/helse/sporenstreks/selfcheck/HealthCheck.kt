package no.nav.helse.sporenstreks.selfcheck

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.system.measureTimeMillis

enum class HealthCheckState { OK, ERROR }
enum class HealthCheckType { READYNESS, ALIVENESS }

data class HealthCheckResult(
        val componentName: String,
        val state: HealthCheckState,
        val runTime: Long,
        val error: Throwable? = null)

interface HealthCheck {
    /**
     * Kjører en selvdiagnose.
     * Skal gi en exception hvis noe feiler.
     * En kjøring uten exception tolkes som OK.
     */
    suspend fun doHealthCheck()

    /**
     * Angir hvilken type helsesjekk denne komponenten utfører, mapper til kubernetes sine readyness og aliveness sjekker
     */
    val healthCheckType: HealthCheckType
}

suspend fun runHealthChecks(checkableComponents: Collection<HealthCheck>): List<HealthCheckResult> {
    return checkableComponents.pmap {
        var runTime = 0L
        try {
            runTime = measureTimeMillis {
                it.doHealthCheck()
            }
            HealthCheckResult(it.javaClass.canonicalName, HealthCheckState.OK, runTime)
        } catch (ex: Throwable) {
            HealthCheckResult(it.javaClass.canonicalName, HealthCheckState.ERROR, runTime, ex)
        }
    }
}

private suspend fun <A, B> Iterable<A>.pmap(f: suspend (A) -> B): List<B> = coroutineScope {
    map { async { f(it) } }.awaitAll()
}
