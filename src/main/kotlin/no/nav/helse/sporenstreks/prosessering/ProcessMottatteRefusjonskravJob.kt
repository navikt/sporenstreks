package no.nav.helse.sporenstreks.prosessering

import kotlinx.coroutines.CoroutineScope
import no.nav.helse.sporenstreks.db.RefusjonskravRepository
import no.nav.helse.sporenstreks.domene.RefusjonskravStatus
import no.nav.helse.sporenstreks.integrasjon.rest.LeaderElection.LeaderElectionConsumer
import java.time.Duration
import java.util.concurrent.locks.ReentrantLock

const val KRAV_TO_PROCESS_LIMIT = 1000


class ProcessMottatteRefusjonskravJob(
        private val db: RefusjonskravRepository,
        private val processor: RefusjonskravBehandler,
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
            return
        }
        mutualLock.lock()
        db.getByStatus(RefusjonskravStatus.MOTTATT, KRAV_TO_PROCESS_LIMIT)
                .forEach {
                    processor.behandle(it)
                    if (shutdownSignalSent) {
                        return@forEach
                    }
                }
        mutualLock.unlock()
    }
}