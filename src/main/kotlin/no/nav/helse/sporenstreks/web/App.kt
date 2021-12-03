package no.nav.helse.sporenstreks.web

import com.typesafe.config.ConfigFactory
import io.ktor.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbService
import no.nav.helse.arbeidsgiver.kubernetes.KubernetesProbeManager
import no.nav.helse.arbeidsgiver.kubernetes.LivenessComponent
import no.nav.helse.arbeidsgiver.kubernetes.ReadynessComponent
import no.nav.helse.arbeidsgiver.system.AppEnv
import no.nav.helse.arbeidsgiver.system.getEnvironment
import no.nav.helse.sporenstreks.auth.localCookieDispenser
import no.nav.helse.sporenstreks.prosessering.kvittering.KvitteringProcessor
import no.nav.helse.sporenstreks.prosessering.metrics.ProcessInfluxJob
import no.nav.helse.sporenstreks.prosessering.refusjonskrav.RefusjonskravProcessor
import org.koin.ktor.ext.getKoin
import org.slf4j.LoggerFactory

val mainLogger = LoggerFactory.getLogger("main")

@KtorExperimentalAPI
fun main() {
    Thread.currentThread().setUncaughtExceptionHandler { thread, err ->
        mainLogger.error("uncaught exception in thread ${thread.name}: ${err.message}", err)
    }

    embeddedServer(Netty, createApplicationEnvironment()).let { app ->
        app.start(wait = false)

        try {
            initBackgroundWorkers(app)
        } catch (ex: Exception) {
            // Ved enhver feil i oppstart av bakgrunnsjobbene dra ned applikasjonen slik at vi blir restartet av kubernetes
            app.stop(1000, 1000)
        }

        Runtime.getRuntime().addShutdownHook(
            Thread {
                app.stop(1000, 1000)
            }
        )
    }
}

private fun initBackgroundWorkers(app: NettyApplicationEngine) {
    val koin = app.application.getKoin()
    if (app.environment.config.getEnvironment() != AppEnv.LOCAL) {
        koin.get<ProcessInfluxJob>().startAsync(retryOnFail = true)
        val bakgrunnsjobbService = koin.get<BakgrunnsjobbService>()
        bakgrunnsjobbService.leggTilBakgrunnsjobbProsesserer(
            KvitteringProcessor.JOBB_TYPE,
            koin.get<KvitteringProcessor>()
        )
        bakgrunnsjobbService.leggTilBakgrunnsjobbProsesserer(
            RefusjonskravProcessor.JOBB_TYPE,
            koin.get<RefusjonskravProcessor>()
        )
        bakgrunnsjobbService.startAsync(true)
    }

    runBlocking { autoDetectProbeableComponents(koin) }
    mainLogger.info("La til probeable komponenter")
}

private suspend fun autoDetectProbeableComponents(koin: org.koin.core.Koin) {
    val kubernetesProbeManager = koin.get<KubernetesProbeManager>()

    koin.getAllOfType<LivenessComponent>()
        .forEach { kubernetesProbeManager.registerLivenessComponent(it) }

    koin.getAllOfType<ReadynessComponent>()
        .forEach { kubernetesProbeManager.registerReadynessComponent(it) }
}

@KtorExperimentalAPI
fun createApplicationEnvironment() = applicationEngineEnvironment {
    config = HoconApplicationConfig(ConfigFactory.load())

    connector {
        port = 8080
    }

    module {
        if (config.getEnvironment() != AppEnv.PROD) {
            localCookieDispenser(config)
        }
        sporenstreksModule(config)
    }
}
