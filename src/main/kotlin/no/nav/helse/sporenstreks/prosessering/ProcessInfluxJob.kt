package no.nav.helse.sporenstreks.prosessering

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.utils.RecurringJob
import no.nav.helse.sporenstreks.db.RefusjonskravRepository
import no.nav.helse.sporenstreks.integrasjon.rest.LeaderElection.LeaderElectionConsumer
import no.nav.helse.sporenstreks.prosessering.metrics.InfluxReporter
import java.util.concurrent.locks.ReentrantLock

const val INFLUX_PROCESS_LIMIT = 250

class ProcessInfluxJob(
        private val db: RefusjonskravRepository,
        coroutineScope: CoroutineScope,
        waitMillisWhenEmptyQueue: Long = (30 * 1000L),
        val leaderElectionConsumer: LeaderElectionConsumer,
        val influxReporter: InfluxReporter
) : RecurringJob(coroutineScope, waitMillisWhenEmptyQueue) {
    var shutdownSignalSent = false
    val mutualLock = ReentrantLock()

    init {
        Runtime.getRuntime().addShutdownHook(Thread {
            stop()
            shutdownSignalSent = true
            mutualLock.lock()
        })
    }

    override fun doJob() {
        if (runBlocking { leaderElectionConsumer.isLeader() })
            return
        mutualLock.lock()
        db.getByIkkeIndeksertInflux(INFLUX_PROCESS_LIMIT)
                .forEach {
                    influxReporter.registerRefusjonskrav(it)
                    it.indeksertInflux = true
                    db.update(it)
                    if (shutdownSignalSent) {
                        return@forEach
                    }
                }
        mutualLock.unlock()
    }
}
