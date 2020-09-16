package no.nav.helse.sporenstreks.prosessering

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.utils.RecurringJob
import no.nav.helse.sporenstreks.db.RefusjonskravRepository
import no.nav.helse.sporenstreks.domene.RefusjonskravStatus
import no.nav.helse.sporenstreks.integrasjon.rest.LeaderElection.LeaderElectionConsumer
import java.util.concurrent.locks.ReentrantLock

const val KRAV_TO_PROCESS_LIMIT = 250


class ProcessMottatteRefusjonskravJob(
        private val db: RefusjonskravRepository,
        private val processor: RefusjonskravBehandler,
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
