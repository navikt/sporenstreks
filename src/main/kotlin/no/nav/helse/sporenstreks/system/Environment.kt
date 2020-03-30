package no.nav.helse.sporenstreks.system

import io.ktor.config.ApplicationConfig
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.sporenstreks.web.getString

enum class AppEnv {
    TEST,
    LOCAL,
    PREPROD,
    PROD
}

@KtorExperimentalAPI
fun ApplicationConfig.getEnvironment(): AppEnv {
    return AppEnv.valueOf(this.getString("koin.profile"))
}