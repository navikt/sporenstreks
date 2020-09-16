package no.nav.helse.sporenstreks.prosessering

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.utils.RecurringJob
import no.nav.helse.sporenstreks.db.KvitteringRepository
import no.nav.helse.sporenstreks.integrasjon.rest.LeaderElection.LeaderElectionConsumer
import no.nav.helse.sporenstreks.kvittering.KvitteringSender
import no.nav.helse.sporenstreks.kvittering.KvitteringStatus
import java.util.concurrent.locks.ReentrantLock

const val KVITTERINGER_TO_PROCESS_LIMIT = 250


class ProcessOpprettedeKvitteringerJob(
        private val db: KvitteringRepository,
        private val kvitteringSender: KvitteringSender,
        coroutineScope: CoroutineScope,
        waitMillisWhenEmptyQueue: Long = (30 * 1000L),
        val leaderElectionConsumer: LeaderElectionConsumer
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
