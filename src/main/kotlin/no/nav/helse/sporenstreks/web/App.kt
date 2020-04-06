package no.nav.helse.sporenstreks.web

import com.typesafe.config.ConfigFactory
import io.ktor.config.HoconApplicationConfig
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.sporenstreks.prosessering.ProcessFeiledeRefusjonskravJob
import no.nav.helse.sporenstreks.prosessering.ProcessMottatteRefusjonskravJob
import no.nav.helse.sporenstreks.system.AppEnv
import no.nav.helse.sporenstreks.system.getEnvironment
import org.koin.ktor.ext.getKoin
import org.slf4j.LoggerFactory


@KtorExperimentalAPI
fun main() {
    Thread.currentThread().setUncaughtExceptionHandler { thread, err ->
        LoggerFactory.getLogger("main")
                .error("uncaught exception in thread ${thread.name}: ${err.message}", err)
    }

    embeddedServer(Netty, createApplicationEnvironment()).let { app ->
        app.start(wait = false)
        val koin = app.application.getKoin()
        if (app.environment.config.getEnvironment() != AppEnv.LOCAL) {
            koin.get<ProcessMottatteRefusjonskravJob>().startAsync(retryOnFail = true)
            koin.get<ProcessFeiledeRefusjonskravJob>().startAsync(retryOnFail = true)
        }
        Runtime.getRuntime().addShutdownHook(Thread {
            app.stop(1000, 1000)
        })
    }
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

