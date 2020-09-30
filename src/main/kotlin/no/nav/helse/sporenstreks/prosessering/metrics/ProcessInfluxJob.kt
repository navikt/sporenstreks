package no.nav.helse.sporenstreks.prosessering.metrics

import kotlinx.coroutines.CoroutineScope
import no.nav.helse.arbeidsgiver.utils.RecurringJob
import no.nav.helse.sporenstreks.db.RefusjonskravRepository

const val INFLUX_PROCESS_LIMIT = 250

class ProcessInfluxJob(
        private val db: RefusjonskravRepository,
        coroutineScope: CoroutineScope,
        waitMillisWhenEmptyQueue: Long = (30 * 1000L),
        val influxReporter: InfluxReporter
) : RecurringJob(coroutineScope, waitMillisWhenEmptyQueue) {


    override fun doJob() {
        db.getByIkkeIndeksertInflux(INFLUX_PROCESS_LIMIT)
                .forEach {
                    influxReporter.registerRefusjonskrav(it)
                    it.indeksertInflux = true
                    db.update(it)
                    if (!isRunning) {
                        return@forEach
                    }
                }
    }
}
