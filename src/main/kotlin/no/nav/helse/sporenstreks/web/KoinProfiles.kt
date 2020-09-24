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
import com.zaxxer.hikari.metrics.prometheus.PrometheusMetricsTrackerFactory
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.json.*
import io.ktor.config.*
import io.ktor.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import no.altinn.services.serviceengine.correspondence._2009._10.ICorrespondenceAgencyExternalBasic
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
import no.nav.helse.sporenstreks.integrasjon.rest.sensu.SensuClient
import no.nav.helse.sporenstreks.integrasjon.rest.sensu.SensuClientImpl
import no.nav.helse.sporenstreks.integrasjon.rest.sts.STSClient
import no.nav.helse.sporenstreks.integrasjon.rest.sts.configureFor
import no.nav.helse.sporenstreks.integrasjon.rest.sts.wsStsClient
import no.nav.helse.sporenstreks.kvittering.*
import no.nav.helse.sporenstreks.prosessering.kvittering.KvitteringJobCreator
import no.nav.helse.sporenstreks.prosessering.metrics.InfluxReporter
import no.nav.helse.sporenstreks.prosessering.metrics.InfluxReporterImpl
import no.nav.helse.sporenstreks.prosessering.metrics.ProcessInfluxJob
import no.nav.helse.sporenstreks.prosessering.refusjonskrav.RefusjonskravJobCreator
import no.nav.helse.sporenstreks.prosessering.refusjonskrav.RefusjonskravProcessor
import no.nav.helse.sporenstreks.service.MockRefusjonskravService
import no.nav.helse.sporenstreks.service.PostgresRefusjonskravService
import no.nav.helse.sporenstreks.service.RefusjonskravService
import org.koin.core.Koin
import org.koin.core.definition.Kind
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module
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
    om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

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
    single { MockKvitteringRepository() as KvitteringRepository }
    single { MockRefusjonskravService(get()) as RefusjonskravService }
    single { MockDokarkivKlient() as DokarkivKlient }
    single { JoarkService(get()) as JoarkService }
    single { OppgaveService(get(), get()) as OppgaveService }
    single { MockOppgaveKlient() as OppgaveKlient }
    single { MockAktorConsumer() as AktorConsumer }
    single { DummyKvitteringSender() as KvitteringSender }

    LocalOIDCWireMock.start()
}

@KtorExperimentalAPI
fun localDevConfig(config: ApplicationConfig) = module {
    single {
        getDataSource(
                createHikariConfig(
                        config.getjdbcUrlFromProperties(),
                        config.getString("database.username"),
                        config.getString("database.password")
                ),
                config.getString("database.name"),
                config.getString("database.vault.mountpath")) as DataSource
    }
    single { PostgresRefusjonskravRepository(get(), get()) as RefusjonskravRepository }
    single { PostgresKvitteringRepository(get(), get()) as KvitteringRepository }
    single { PostgresRefusjonskravService(get(), get(), get(), get(), get()) as RefusjonskravService }

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
    single { DummyKvitteringSender() as KvitteringSender }
    LocalOIDCWireMock.start()
}

@KtorExperimentalAPI
fun preprodConfig(config: ApplicationConfig) = module {
    single {
        getDataSource(createHikariConfig(config.getjdbcUrlFromProperties(), prometheusMetricsTrackerFactory = PrometheusMetricsTrackerFactory()),
                config.getString("database.name"),
                config.getString("database.vault.mountpath")) as DataSource
    }
    single { PostgresRefusjonskravRepository(get(), get()) as RefusjonskravRepository }
    single { PostgresKvitteringRepository(get(), get()) as KvitteringRepository }
    single { PostgresRefusjonskravService(get(), get(), get(), get(), get()) as RefusjonskravService }

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

    single {SensuClientImpl("sensu.nais", 3030) as SensuClient }
    single {InfluxReporterImpl("sporenstreks", "dev-fss", "default", get()) as InfluxReporter}

    single { STSClient(config.getString("service_user.username"), config.getString("service_user.password"), config.getString("sts_url_rest")) }
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



    single {
        val altinnMeldingWsClient = Clients.iCorrespondenceExternalBasic(
                config.getString("altinn_melding.pep_gw_endpoint")
        )
        val sts = wsStsClient(
                config.getString("sts_url_ws"),
                config.getString("service_user.username") to config.getString("service_user.password")
        )
        sts.configureFor(altinnMeldingWsClient)
        altinnMeldingWsClient as ICorrespondenceAgencyExternalBasic
    }

    single {
        AltinnKvitteringSender(
                AltinnKvitteringMapper(config.getString("altinn_melding.service_id")),
                get(),
                config.getString("altinn_melding.username"),
                config.getString("altinn_melding.password"),
                get())
                as KvitteringSender
    }

    single { RefusjonskravProcessor(get(), get(), get(), get(), get()) }
    single { RefusjonskravJobCreator(get(), get(), get(), get(), CoroutineScope(Dispatchers.IO), 1000 * 60 * 60 * 5) }
    single { ProcessInfluxJob(get(), CoroutineScope(Dispatchers.IO), 1000 * 60, get()) }
    single { KvitteringJobCreator(get(), get(), get(), get(), CoroutineScope(Dispatchers.IO), 1000 * 60 * 10) }
}

@KtorExperimentalAPI
fun prodConfig(config: ApplicationConfig) = module {
    single {
        getDataSource(createHikariConfig(config.getjdbcUrlFromProperties(), prometheusMetricsTrackerFactory = PrometheusMetricsTrackerFactory()),
                config.getString("database.name"),
                config.getString("database.vault.mountpath")
        ) as DataSource
    }

    single {
        val altinn = AltinnClient(
                config.getString("altinn.service_owner_api_url"),
                config.getString("altinn.gw_api_key"),
                config.getString("altinn.altinn_api_key"),
                config.getString("altinn.service_id"),
                get()
        )

        CachedAuthRepo(altinn) as AuthorizationsRepository
    }

    single {SensuClientImpl("sensu.nais", 3030) as SensuClient }
    single {InfluxReporterImpl("sporenstreks", "prod-fss", "default", get()) as InfluxReporter}
    single { STSClient(config.getString("service_user.username"), config.getString("service_user.password"), config.getString("sts_url_rest")) }
    single { DokarkivKlientImpl(config.getString("dokarkiv.base_url"), get(), get()) as DokarkivKlient }
    single { PostgresRefusjonskravRepository(get(), get()) as RefusjonskravRepository }
    single { PostgresKvitteringRepository(get(), get()) as KvitteringRepository }
    single { PostgresRefusjonskravService(get(), get(), get(), get(), get()) as RefusjonskravService }
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

    single {
        val altinnMeldingWsClient = Clients.iCorrespondenceExternalBasic(
                config.getString("altinn_melding.pep_gw_endpoint")
        )
        val sts = wsStsClient(
                config.getString("sts_url_ws"),
                config.getString("service_user.username") to config.getString("service_user.password")
        )
        sts.configureFor(altinnMeldingWsClient)
        altinnMeldingWsClient as ICorrespondenceAgencyExternalBasic
    }

    single {
        AltinnKvitteringSender(
                AltinnKvitteringMapper(config.getString("altinn_melding.service_id")),
                get(),
                config.getString("altinn_melding.username"),
                config.getString("altinn_melding.password"),
                get()
        )
                as KvitteringSender
    }

    single { RefusjonskravProcessor(get(), get(), get(), get(), get()) }
    single { RefusjonskravJobCreator(get(), get(), get(), get(), CoroutineScope(Dispatchers.IO), 1000 * 60 * 60 * 2) }
    single { KvitteringJobCreator(get(), get(), get(), get(), CoroutineScope(Dispatchers.IO), 1000 * 60 * 60 * 2) }
    single { ProcessInfluxJob(get(), CoroutineScope(Dispatchers.IO), 1000 * 60 * 2, get()) }
}

// utils
@KtorExperimentalAPI
fun ApplicationConfig.getString(path: String): String {
    return this.property(path).getString()
}

@KtorExperimentalAPI
fun ApplicationConfig.getjdbcUrlFromProperties(): String {
    return String.format("jdbc:postgresql://%s:%s/%s?reWriteBatchedInserts=true",
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
