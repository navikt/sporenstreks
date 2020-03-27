package no.nav.helse.sporenstreks.metrics

import io.prometheus.client.Counter
import io.prometheus.client.Summary

const val METRICS_NS = "sporenstreks"

val REQUEST_TIME: Summary = Summary.build()
        .namespace(METRICS_NS)
        .name("request_time_ms")
        .help("Request time in milliseconds.").register()

val INNKOMMENDE_REFUSJONSKRAV_COUNTER: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name("inkommende_refusjonskrav")
        .help("Counts the number of incoming messages")
        .register()
