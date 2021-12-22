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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import no.altinn.services.serviceengine.correspondence._2009._10.ICorrespondenceAgencyExternalBasic
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbService
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.MockBakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.PostgresBakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.integrasjoner.AccessTokenProvider
import no.nav.helse.arbeidsgiver.integrasjoner.RestSTSAccessTokenProvider
import no.nav.helse.arbeidsgiver.integrasjoner.aareg.AaregArbeidsforholdClient
import no.nav.helse.arbeidsgiver.integrasjoner.aareg.AaregArbeidsforholdClientImpl
import no.nav.helse.arbeidsgiver.integrasjoner.altinn.AltinnRestClient
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.DokarkivKlient
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.DokarkivKlientImpl
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OppgaveKlient
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OppgaveKlientImpl
import no.nav.helse.arbeidsgiver.kubernetes.KubernetesProbeManager
import no.nav.helse.arbeidsgiver.system.getString
import no.nav.helse.arbeidsgiver.web.auth.AltinnAuthorizer
import no.nav.helse.arbeidsgiver.web.auth.AltinnOrganisationsRepository
import no.nav.helse.arbeidsgiver.web.auth.DefaultAltinnAuthorizer
import no.nav.helse.sporenstreks.auth.*
import no.nav.helse.sporenstreks.db.*
import no.nav.helse.sporenstreks.integrasjon.JoarkService
import no.nav.helse.sporenstreks.integrasjon.OppgaveService
import no.nav.helse.sporenstreks.integrasjon.rest.MockAaregArbeidsforholdClient
import no.nav.helse.sporenstreks.integrasjon.rest.aktor.AktorConsumer
import no.nav.helse.sporenstreks.integrasjon.rest.aktor.AktorConsumerImpl
import no.nav.helse.sporenstreks.integrasjon.rest.aktor.MockAktorConsumer
import no.nav.helse.sporenstreks.integrasjon.rest.dokarkiv.MockDokarkivKlient
import no.nav.helse.sporenstreks.integrasjon.rest.oppgave.MockOppgaveKlient
import no.nav.helse.sporenstreks.integrasjon.rest.sensu.SensuClient
import no.nav.helse.sporenstreks.integrasjon.rest.sensu.SensuClientImpl
import no.nav.helse.sporenstreks.integrasjon.rest.sts.configureFor
import no.nav.helse.sporenstreks.integrasjon.rest.sts.wsStsClient
import no.nav.helse.sporenstreks.kvittering.*
import no.nav.helse.sporenstreks.metrics.MetrikkVarsler
import no.nav.helse.sporenstreks.prosessering.kvittering.KvitteringProcessor
import no.nav.helse.sporenstreks.prosessering.metrics.InfluxReporter
import no.nav.helse.sporenstreks.prosessering.metrics.InfluxReporterImpl
import no.nav.helse.sporenstreks.prosessering.metrics.ProcessInfluxJob
import no.nav.helse.sporenstreks.prosessering.refusjonskrav.RefusjonskravProcessor
import no.nav.helse.sporenstreks.service.MockRefusjonskravService
import no.nav.helse.sporenstreks.service.PostgresRefusjonskravService
import no.nav.helse.sporenstreks.service.RefusjonskravService
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module
import javax.sql.DataSource

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

    om.setDefaultPrettyPrinter(
        DefaultPrettyPrinter().apply {
            indentArraysWith(DefaultPrettyPrinter.FixedSpaceIndenter.instance)
            indentObjectsWith(DefaultIndenter("  ", "\n"))
        }
    )

    single { om }

    single { KubernetesProbeManager() }

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
    single<AltinnOrganisationsRepository> { StaticMockAuthRepo(get()) } bind StaticMockAuthRepo::class
    single<AltinnAuthorizer> { DefaultAltinnAuthorizer(get()) }
    single<RefusjonskravRepository> { MockRefusjonskravRepo() }
    single<KvitteringRepository> { MockKvitteringRepository() }
    single<RefusjonskravService> { MockRefusjonskravService(get()) }
    single<BakgrunnsjobbRepository> { MockBakgrunnsjobbRepository() }
    single<DokarkivKlient> { MockDokarkivKlient() }
    single<AaregArbeidsforholdClient> { MockAaregArbeidsforholdClient() }
    single<JoarkService> { JoarkService(get()) }
    single<OppgaveService> { OppgaveService(get(), get()) }
    single<OppgaveKlient> { MockOppgaveKlient() }
    single<AktorConsumer> { MockAktorConsumer() }
    single<KvitteringSender> { DummyKvitteringSender() }
    single { BakgrunnsjobbService(bakgrunnsjobbRepository = get(), bakgrunnsvarsler = MetrikkVarsler()) }
}

fun localDevConfig(config: ApplicationConfig) = module {
    single<DataSource> {
        getDataSource(
            createHikariConfig(
                config.getjdbcUrlFromProperties(),
                config.getString("database.username"),
                config.getString("database.password")
            ),
            config.getString("database.name"),
            config.getString("database.vault.mountpath")
        )
    }
    single<RefusjonskravRepository> { PostgresRefusjonskravRepository(get(), get()) }
    single<KvitteringRepository> { PostgresKvitteringRepository(get(), get()) }
    single<RefusjonskravService> { PostgresRefusjonskravService(get(), get(), get(), get(), get()) }
    single<BakgrunnsjobbRepository> { PostgresBakgrunnsjobbRepository(get()) }

    single<DokarkivKlient> { MockDokarkivKlient() }
    single<AaregArbeidsforholdClient> { MockAaregArbeidsforholdClient() }
    single<AltinnOrganisationsRepository> { StaticMockAuthRepo(get()) }
    single<AltinnAuthorizer> { DefaultAltinnAuthorizer(get()) }
    single<JoarkService> { JoarkService(get()) }
    single { BakgrunnsjobbService(bakgrunnsjobbRepository = get(), bakgrunnsvarsler = MetrikkVarsler()) }

    single<AktorConsumer> { MockAktorConsumer() }
    single<OppgaveKlient> { MockOppgaveKlient() }
    single<OppgaveService> { OppgaveService(get(), get()) }
    single<KvitteringSender> { DummyKvitteringSender() }
}

fun preprodConfig(config: ApplicationConfig) = module {
    single<DataSource> {
        getDataSource(
            createHikariConfig(config.getjdbcUrlFromProperties(), prometheusMetricsTrackerFactory = PrometheusMetricsTrackerFactory()),
            config.getString("database.name"),
            config.getString("database.vault.mountpath")
        )
    }
    single<RefusjonskravRepository> { PostgresRefusjonskravRepository(get(), get()) }
    single<KvitteringRepository> { PostgresKvitteringRepository(get(), get()) }
    single<RefusjonskravService> { PostgresRefusjonskravService(get(), get(), get(), get(), get()) }
    single<BakgrunnsjobbRepository> { PostgresBakgrunnsjobbRepository(get()) }

    single<AltinnOrganisationsRepository> {
        val altinnClient = AltinnRestClient(
            config.getString("altinn.service_owner_api_url"),
            config.getString("altinn.gw_api_key"),
            config.getString("altinn.altinn_api_key"),
            config.getString("altinn.service_id"),
            get()
        )

        CachedAuthRepo(altinnClient)
    }

    single<SensuClient> { SensuClientImpl("sensu.nais", 3030) }
    single<InfluxReporter> { InfluxReporterImpl("sporenstreks", "dev-fss", "default", get()) }

    single<AccessTokenProvider> {
        RestSTSAccessTokenProvider(
            config.getString("service_user.username"),
            config.getString("service_user.password"),
            config.getString("sts_url_rest"),
            get()
        )
    }
    single<DokarkivKlient> { DokarkivKlientImpl(config.getString("dokarkiv.base_url"), get(), get()) }
    single {
        AaregArbeidsforholdClientImpl(
            config.getString("aareg_url") + "/api/v1/arbeidstaker/arbeidsforhold?sporingsinformasjon=false&historikk=false",
            get(),
            get()
        )
    } bind AaregArbeidsforholdClient::class
    single<JoarkService> { JoarkService(get()) }
    single { BakgrunnsjobbService(bakgrunnsjobbRepository = get(), bakgrunnsvarsler = MetrikkVarsler()) }

    single<AltinnAuthorizer> { DefaultAltinnAuthorizer(get()) }
    single<AktorConsumer> {
        AktorConsumerImpl(
            get(),
            config.getString("service_user.username"),
            config.getString("aktoerregister.url"),
            get()
        )
    }
    single<OppgaveKlient> { OppgaveKlientImpl(config.getString("oppgavebehandling.url"), get(), get()) }
    single<OppgaveService> { OppgaveService(get(), get()) }

    single<ICorrespondenceAgencyExternalBasic> {
        val altinnMeldingWsClient = Clients.iCorrespondenceExternalBasic(
            config.getString("altinn_melding.pep_gw_endpoint")
        )
        val sts = wsStsClient(
            config.getString("sts_url_ws"),
            config.getString("service_user.username") to config.getString("service_user.password")
        )
        sts.configureFor(altinnMeldingWsClient)
        altinnMeldingWsClient
    }

    single<KvitteringSender> {
        AltinnKvitteringSender(
            AltinnKvitteringMapper(config.getString("altinn_melding.service_id")),
            get(),
            config.getString("altinn_melding.username"),
            config.getString("altinn_melding.password"),
            get()
        )
    }

    single { RefusjonskravProcessor(get(), get(), get(), get(), get()) }
    single { KvitteringProcessor(get(), get(), get()) }
    single { ProcessInfluxJob(get(), CoroutineScope(Dispatchers.IO), (1000 * 60).toLong(), get()) }
}

fun prodConfig(config: ApplicationConfig) = module {
    single<DataSource> {
        getDataSource(
            createHikariConfig(config.getjdbcUrlFromProperties(), prometheusMetricsTrackerFactory = PrometheusMetricsTrackerFactory()),
            config.getString("database.name"),
            config.getString("database.vault.mountpath")
        )
    }

    single<AltinnOrganisationsRepository> {
        val altinn = AltinnRestClient(
            config.getString("altinn.service_owner_api_url"),
            config.getString("altinn.gw_api_key"),
            config.getString("altinn.altinn_api_key"),
            config.getString("altinn.service_id"),
            get()
        )

        CachedAuthRepo(altinn)
    }

    single<SensuClient> { SensuClientImpl("sensu.nais", 3030) }
    single<InfluxReporter> { InfluxReporterImpl("sporenstreks", "prod-fss", "default", get()) }
    single<AccessTokenProvider> {
        RestSTSAccessTokenProvider(
            config.getString("service_user.username"),
            config.getString("service_user.password"),
            config.getString("sts_url_rest"),
            get()
        )
    }
    single<DokarkivKlient> { DokarkivKlientImpl(config.getString("dokarkiv.base_url"), get(), get()) }
    single<RefusjonskravRepository> { PostgresRefusjonskravRepository(get(), get()) }
    single<KvitteringRepository> { PostgresKvitteringRepository(get(), get()) }
    single<RefusjonskravService> { PostgresRefusjonskravService(get(), get(), get(), get(), get()) }
    single<BakgrunnsjobbRepository> { PostgresBakgrunnsjobbRepository(get()) }
    single<JoarkService> { JoarkService(get()) }
    single {
        AaregArbeidsforholdClientImpl(
            config.getString("aareg_url") + "/api/v1/arbeidstaker/arbeidsforhold?sporingsinformasjon=false&historikk=false",
            get(),
            get()
        )
    } bind AaregArbeidsforholdClient::class
    single { BakgrunnsjobbService(bakgrunnsjobbRepository = get(), bakgrunnsvarsler = MetrikkVarsler()) }
    single<AltinnAuthorizer> { DefaultAltinnAuthorizer(get()) }
    single<OppgaveKlient> { OppgaveKlientImpl(config.getString("oppgavebehandling.url"), get(), get()) }
    single<OppgaveService> { OppgaveService(get(), get()) }
    single<AktorConsumer> {
        AktorConsumerImpl(
            get(),
            config.getString("service_user.username"),
            config.getString("aktoerregister.url"),
            get()
        )
    }

    single<ICorrespondenceAgencyExternalBasic> {
        val altinnMeldingWsClient = Clients.iCorrespondenceExternalBasic(
            config.getString("altinn_melding.pep_gw_endpoint")
        )
        val sts = wsStsClient(
            config.getString("sts_url_ws"),
            config.getString("service_user.username") to config.getString("service_user.password")
        )
        sts.configureFor(altinnMeldingWsClient)
        altinnMeldingWsClient
    }

    single<KvitteringSender> {
        AltinnKvitteringSender(
            AltinnKvitteringMapper(config.getString("altinn_melding.service_id")),
            get(),
            config.getString("altinn_melding.username"),
            config.getString("altinn_melding.password"),
            get()
        )
    }

    single { RefusjonskravProcessor(get(), get(), get(), get(), get()) }
    single { KvitteringProcessor(get(), get(), get()) }
    single { ProcessInfluxJob(get(), CoroutineScope(Dispatchers.IO), (1000 * 60 * 2).toLong(), get()) }
}

// utils
fun ApplicationConfig.getString(path: String): String {
    return this.property(path).getString()
}

fun ApplicationConfig.getjdbcUrlFromProperties(): String {
    return String.format(
        "jdbc:postgresql://%s:%s/%s?reWriteBatchedInserts=true",
        this.property("database.host").getString(),
        this.property("database.port").getString(),
        this.property("database.name").getString()
    )
}
