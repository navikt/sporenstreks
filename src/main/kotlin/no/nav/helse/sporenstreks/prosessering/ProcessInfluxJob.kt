package no.nav.helse.sporenstreks.prosessering

import kotlinx.coroutines.CoroutineScope
import no.nav.helse.sporenstreks.db.RefusjonskravRepository
import no.nav.helse.sporenstreks.integrasjon.rest.LeaderElection.LeaderElectionConsumer
import no.nav.helse.sporenstreks.prosessering.metrics.InfluxReporter
import java.time.Duration
import java.util.concurrent.locks.ReentrantLock

const val INFLUX_PROCESS_LIMIT = 250

class ProcessInfluxJob(
        private val db: RefusjonskravRepository,
        coroutineScope: CoroutineScope,
        freq: Duration,
        val leaderElectionConsumer: LeaderElectionConsumer,
        val influxReporter: InfluxReporter
) : RecurringJob(coroutineScope, freq) {
    var shutdownSignalSent = false
    val mutualLock = ReentrantLock()

    init {
        Runtime.getRuntime().addShutdownHook(Thread {
            stop()
            shutdownSignalSent = true
            mutualLock.lock()
        })
    }

    override suspend fun doJob() {
        if (!leaderElectionConsumer.isLeader()) {
            logger.info("Er ikke leader")
            return
        }
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
