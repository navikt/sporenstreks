package no.nav.helse.sporenstreks.web

import com.typesafe.config.ConfigFactory
import io.ktor.config.HoconApplicationConfig
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.kubernetes.KubernetesProbeManager
import no.nav.helse.arbeidsgiver.kubernetes.LivenessComponent
import no.nav.helse.arbeidsgiver.kubernetes.ReadynessComponent
import no.nav.helse.sporenstreks.prosessering.*
import no.nav.helse.sporenstreks.system.AppEnv
import no.nav.helse.sporenstreks.system.getEnvironment
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
        val koin = app.application.getKoin()
        if (app.environment.config.getEnvironment() != AppEnv.LOCAL) {
            koin.get<ProcessMottatteRefusjonskravJob>().startAsync(retryOnFail = true)
            koin.get<ProcessFeiledeRefusjonskravJob>().startAsync(retryOnFail = true)
            koin.get<ProcessInfluxJob>().startAsync(retryOnFail = true)
            koin.get<ProcessOpprettedeKvitteringerJob>().startAsync(retryOnFail = true)
            koin.get<ProcessFeiledeKvitteringerJob>().startAsync(retryOnFail = true)
            koin.get<SendKvitteringForEksisterendeKravJob>().startAsync(retryOnFail = true)

        }

        runBlocking { autoDetectProbeableComponents(koin) }
        mainLogger.info("La til probeable komponenter")

        Runtime.getRuntime().addShutdownHook(Thread {
            app.stop(1000, 1000)
        })
    }
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
        sporenstreksModule(config)
    }
}

