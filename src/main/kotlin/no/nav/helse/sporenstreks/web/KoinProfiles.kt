package no.nav.helse.sporenstreks.web

import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.config.ApplicationConfig
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import no.nav.helse.sporenstreks.auth.*
import no.nav.helse.sporenstreks.auth.altinn.AltinnClient
import no.nav.helse.sporenstreks.db.*
import no.nav.helse.sporenstreks.integrasjon.JoarkService
import no.nav.helse.sporenstreks.integrasjon.OppgaveService
import no.nav.helse.sporenstreks.integrasjon.rest.aktor.AktorConsumer
import no.nav.helse.sporenstreks.integrasjon.rest.aktor.AktorConsumerImpl
import no.nav.helse.sporenstreks.integrasjon.rest.aktor.MockAktorConsumer
import no.nav.helse.sporenstreks.integrasjon.rest.dokarkiv.DokarkivKlient
import no.nav.helse.sporenstreks.integrasjon.rest.dokarkiv.DokarkivKlientImpl
import no.nav.helse.sporenstreks.integrasjon.rest.dokarkiv.MockDokarkivKlient
import no.nav.helse.sporenstreks.integrasjon.rest.oppgave.MockOppgaveKlient
import no.nav.helse.sporenstreks.integrasjon.rest.oppgave.OppgaveKlient
import no.nav.helse.sporenstreks.integrasjon.rest.oppgave.OppgaveKlientImpl
import no.nav.helse.sporenstreks.integrasjon.rest.sts.STSClient
import no.nav.helse.sporenstreks.prosessering.ProcessFeiledeRefusjonskravJob
import no.nav.helse.sporenstreks.prosessering.ProcessMottatteRefusjonskravJob
import no.nav.helse.sporenstreks.prosessering.RefusjonskravBehandler
import org.koin.core.Koin
import org.koin.core.definition.Kind
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module
import java.time.Duration
import javax.sql.DataSource


@KtorExperimentalAPI
fun selectModuleBasedOnProfile(config: ApplicationConfig): List<Module> {
    val envModule = when (config.property("koin.profile").getString()) {
        "TEST" -> buildAndTestConfig()
        "LOCAL" -> localDevConfig(config)
        "PREPROD" -> preprodConfig(config)
        "PROD" -> prodConfig(config)
        else -> localDevConfig(config)
    }
    return listOf(common, envModule)
}

val common = module {
    val om = ObjectMapper()
    om.registerModule(KotlinModule())
    om.registerModule(Jdk8Module())
    om.registerModule(JavaTimeModule())
    om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    om.configure(SerializationFeature.INDENT_OUTPUT, true)
    om.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)

    om.setDefaultPrettyPrinter(DefaultPrettyPrinter().apply {
        indentArraysWith(DefaultPrettyPrinter.FixedSpaceIndenter.instance)
        indentObjectsWith(DefaultIndenter("  ", "\n"))
    })

    single { om }

    val httpClient = HttpClient(Apache) {
        install(JsonFeature) {
            serializer = JacksonSerializer {
                registerModule(KotlinModule())
                registerModule(Jdk8Module())
                registerModule(JavaTimeModule())
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                configure(SerializationFeature.INDENT_OUTPUT, true)
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
            }
        }
    }

    single { httpClient }

}

fun buildAndTestConfig() = module {
    single { StaticMockAuthRepo(get()) as AuthorizationsRepository } bind StaticMockAuthRepo::class
    single { DefaultAuthorizer(get()) as Authorizer }
    single { MockRefusjonskravRepo() as RefusjonskravRepository }
    single { MockDokarkivKlient() as DokarkivKlient }
    single { JoarkService(get()) as JoarkService }
    single { OppgaveService(get(), get()) as OppgaveService }
    single { MockOppgaveKlient() as OppgaveKlient }
    single { MockAktorConsumer() as AktorConsumer }

    LocalOIDCWireMock.start()
}

fun localDevConfig(config: ApplicationConfig) = module {
    single { getDataSource(createLocalHikariConfig(), "sporenstreks", null) as DataSource }
    single { PostgresRefusjonskravRepository(get(), get()) as RefusjonskravRepository }

    single { MockDokarkivKlient() as DokarkivKlient }
    single { StaticMockAuthRepo(get()) as AuthorizationsRepository }
    single { DefaultAuthorizer(get()) as Authorizer }
    single { JoarkService(get()) as JoarkService }
    single {
        AktorConsumerImpl(get(),
                config.getString("service_user.username"),
                config.getString("aktoerregister.url"),
                get()
        ) as AktorConsumer
    }
    single { OppgaveService(get(), get()) as OppgaveService }
    single { OppgaveKlientImpl(config.getString("oppgavebehandling.url"), get(), get()) as OppgaveKlient }

    LocalOIDCWireMock.start()
}

@KtorExperimentalAPI
fun preprodConfig(config: ApplicationConfig) = module {
    single {
        getDataSource(createHikariConfig(config.getjdbcUrlFromProperties()),
                config.getString("database.name"),
                config.getString("database.vault.mountpath")) as DataSource
    }
    single { PostgresRefusjonskravRepository(get(), get()) as RefusjonskravRepository }

    single {
        val altinnClient = AltinnClient(
                config.getString("altinn.service_owner_api_url"),
                config.getString("altinn.gw_api_key"),
                config.getString("altinn.altinn_api_key"),
                config.getString("altinn.service_id"),
                get()
        )

        CachedAuthRepo(altinnClient) as AuthorizationsRepository
    }

    single { STSClient(config.getString("service_user.username"), config.getString("service_user.password"), config.getString("sts_url")) }
    single { DokarkivKlientImpl(config.getString("dokarkiv.base_url"), get(), get()) as DokarkivKlient }
    single { JoarkService(get()) as JoarkService }

    single { DefaultAuthorizer(get()) as Authorizer }
    single {
        AktorConsumerImpl(get(),
                config.getString("service_user.username"),
                config.getString("aktoerregister.url"),
                get()
        ) as AktorConsumer
    }
    single { OppgaveKlientImpl(config.getString("oppgavebehandling.url"), get(), get()) as OppgaveKlient }
    single { OppgaveService(get(), get()) as OppgaveService }

    single { RefusjonskravBehandler(get(), get(), get(), get())}
    single { ProcessMottatteRefusjonskravJob(get(), get(), CoroutineScope(Dispatchers.IO), Duration.ofMinutes(1)) }
    single { ProcessFeiledeRefusjonskravJob(get(), get(), CoroutineScope(Dispatchers.IO), Duration.ofMinutes(10)) }

}

@KtorExperimentalAPI
fun prodConfig(config: ApplicationConfig) = module {
    single {
        getDataSource(createHikariConfig(config.getjdbcUrlFromProperties()),
                config.getString("database.name"),
                config.getString("database.vault.mountpath")) as DataSource
    }

    single {
        AltinnClient(
                config.getString("altinn.service_owner_api_url"),
                config.getString("altinn.gw_api_key"),
                config.getString("altinn.altinn_api_key"),
                config.getString("altinn.service_id"),
                get()
        ) as AuthorizationsRepository
    }

    single { STSClient(config.getString("service_user.username"), config.getString("service_user.password"), config.getString("sts_url")) }
    single { DokarkivKlientImpl(config.getString("dokarkiv.base_url"), get(), get()) as DokarkivKlient }
    single { PostgresRefusjonskravRepository(get(), get()) as RefusjonskravRepository }
    single { JoarkService(get()) as JoarkService }
    single { DefaultAuthorizer(get()) as Authorizer }
    single { OppgaveKlientImpl(config.getString("oppgavebehandling.url"), get(), get()) as OppgaveKlient }
    single { OppgaveService(get(), get()) as OppgaveService }
    single {
        AktorConsumerImpl(get(),
                config.getString("service_user.username"),
                config.getString("aktoerregister.url"),
                get()
        ) as AktorConsumer
    }

    single { ProcessMottatteRefusjonskravJob(get(), get(), CoroutineScope(Dispatchers.IO), Duration.ofMinutes(1)) }
    single { ProcessFeiledeRefusjonskravJob(get(), get(), CoroutineScope(Dispatchers.IO), Duration.ofMinutes(10)) }
}

// utils
@KtorExperimentalAPI
fun ApplicationConfig.getString(path: String): String {
    return this.property(path).getString()
}

@KtorExperimentalAPI
fun ApplicationConfig.getjdbcUrlFromProperties(): String {
    return String.format("jdbc:postgresql://%s:%s/%s",
            this.property("database.host").getString(),
            this.property("database.port").getString(),
            this.property("database.name").getString())
}

inline fun <reified T : Any> Koin.getAllOfType(): Collection<T> =
        let { koin ->
            koin.rootScope.beanRegistry
                    .getAllDefinitions()
                    .filter { it.kind == Kind.Single }
                    .map { koin.get<Any>(clazz = it.primaryType, qualifier = null, parameters = null) }
                    .filterIsInstance<T>()
        }