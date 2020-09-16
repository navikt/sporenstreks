package no.nav.helse.sporenstreks.prosessering

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.utils.RecurringJob
import no.nav.helse.sporenstreks.db.RefusjonskravRepository
import no.nav.helse.sporenstreks.integrasjon.rest.LeaderElection.LeaderElectionConsumer
import no.nav.helse.sporenstreks.service.RefusjonskravService
import org.slf4j.LoggerFactory
import java.util.concurrent.locks.ReentrantLock


class SendKvitteringForEksisterendeKravJob(
        private val refusjonskravService: RefusjonskravService,
        private val refusjonskravRepository: RefusjonskravRepository,
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

        refusjonskravRepository.getRandomVirksomhetWithoutKvittering()?.let {
            val kravListe = refusjonskravRepository.getAllForVirksomhetWithoutKvittering(it)
            logger.info("Oppretter kvittering for ${kravListe.size} krav")
            refusjonskravService.updateKravListWithKvittering(kravListe)
        }
        mutualLock.unlock()
    }

    companion object {
        private val log = LoggerFactory.getLogger(SendKvitteringForEksisterendeKravJob::class.java)
    }
}
