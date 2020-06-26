package no.nav.helse.sporenstreks.prosessering

import kotlinx.coroutines.CoroutineScope
import no.nav.helse.sporenstreks.db.KvitteringRepository
import no.nav.helse.sporenstreks.integrasjon.rest.LeaderElection.LeaderElectionConsumer
import no.nav.helse.sporenstreks.kvittering.KvitteringSender
import no.nav.helse.sporenstreks.kvittering.KvitteringStatus
import java.time.Duration
import java.util.concurrent.locks.ReentrantLock

const val KVITTERINGER_TO_PROCESS_LIMIT = 250


class ProcessOpprettedeKvitteringerJob(
        private val db: KvitteringRepository,
        private val kvitteringSender: KvitteringSender,
        coroutineScope: CoroutineScope,
        freq: Duration,
        val leaderElectionConsumer: LeaderElectionConsumer
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
            logger.warn("Er ikke leader")
            return
        }
        mutualLock.lock()
        db.getByStatus(KvitteringStatus.OPPRETTET, KVITTERINGER_TO_PROCESS_LIMIT)
                .forEach {
                    kvitteringSender.send(it)
                    if (shutdownSignalSent) {
                        return@forEach
                    }
                }
        mutualLock.unlock()
    }
}