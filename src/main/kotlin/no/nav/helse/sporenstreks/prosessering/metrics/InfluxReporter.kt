package no.nav.helse.sporenstreks.prosessering.metrics

import no.nav.helse.sporenstreks.domene.Refusjonskrav
import no.nav.helse.sporenstreks.integrasjon.rest.sensu.SensuClient
import no.nav.helse.sporenstreks.integrasjon.rest.sensu.SensuEvent
import no.nav.helse.sporenstreks.prosessering.RefusjonskravBehandler
import org.influxdb.dto.Point
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

interface InfluxReporter {
    fun registerRefusjonskrav(krav: Refusjonskrav)
}

class InfluxReporterImpl(
        private val appName: String,
        private val cluster: String,
        private val namespace: String,
        private val sensuClient: SensuClient
) : InfluxReporter {

    val logger = LoggerFactory.getLogger(InfluxReporter::class.java)

    fun registerPoint(measurement: String, tags: Map<String, String>, fields: Map<String, Any>, nanos: Long) {
        sensuClient.write(createSensuEvent(Point.measurement(measurement)
                .time(nanos, TimeUnit.NANOSECONDS)
                .tag(tags)
                .tag(DEFAULT_TAGS)
                .fields(fields)
                .build().lineProtocol()))
    }

    private val DEFAULT_TAGS: Map<String, String> = java.util.Map.of(
            "application", appName,
            "cluster", cluster,
            "namespace", namespace
    )

    fun createSensuEvent(output: String): SensuEvent =
            SensuEvent("helsearbeidsgiver-sporenstreks-events", output)

    override fun registerRefusjonskrav(krav: Refusjonskrav) {
        registerPoint(
                measurement = "sporenstreks.refusjonskrav",
                tags = mapOf("kilde" to krav.kilde),
                fields = mapOf(
                        "antallPerioder" to krav.perioder.size,
                        "antallDagerRefusjon" to krav.perioder.sumBy { it.antallDagerMedRefusjon },
                        "totalBeløp" to krav.perioder.sumByDouble { it.beloep }),
                nanos = krav.opprettet.nano.toLong())
        logger.info("registrerer info om krav ${krav.id} til influx")
    }
}
