package no.nav.helse.sporenstreks.metrics

import io.prometheus.client.Counter
import io.prometheus.client.Summary
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.Bakgrunnsvarsler

const val METRICS_NS = "sporenstreks"

class MetrikkVarsler() : Bakgrunnsvarsler {

    val FEILET_JOBB_COUNTER = Counter.build().namespace(METRICS_NS)
            .name("feil")
            .help("Counts the number of errors")
            .register()

    override fun rapporterPermanentFeiletJobb() {
        FEILET_JOBB_COUNTER.inc()
    }
}

val INNKOMMENDE_REFUSJONSKRAV_COUNTER: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name("inkommende_refusjonskrav")
        .help("Counts the number of incoming messages")
        .register()

val INNKOMMENDE_REFUSJONSKRAV_BELOEP_COUNTER: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name("sum_refusjonskrav")
        .help("Counts the number of incoming messages")
        .register()

val JOURNALFOERING_COUNTER: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name("journalfoering")
        .help("Counts number of created journalposts")
        .register()

val OPPGAVE_COUNTER: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name("oppgave")
        .help("Counts number of created oppgaves")
        .register()


val FEIL_COUNTER: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name("feil")
        .help("Counts the number of errors")
        .register()

val TEST_COUNTER: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name("test")
        .help("Counts the number of tests")
        .register()

val REQUEST_TIME: Summary = Summary.build()
        .namespace(METRICS_NS)
        .name("request_time_ms")
        .help("Request time in milliseconds.").register()

val KRAV_TIME: Summary = Summary.build()
        .namespace(METRICS_NS)
        .name("krav_time_ms")
        .help("Krav time in milliseconds.").register()



