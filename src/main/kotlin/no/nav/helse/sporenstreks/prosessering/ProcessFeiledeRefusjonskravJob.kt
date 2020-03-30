package no.nav.helse.sporenstreks.prosessering

import kotlinx.coroutines.CoroutineScope
import no.nav.helse.sporenstreks.db.RefusjonskravRepository
import no.nav.helse.sporenstreks.domene.RefusjonskravStatus
import java.time.Duration
import java.util.concurrent.locks.ReentrantLock

class ProcessFeiledeRefusjonskravJob(
        private val db: RefusjonskravRepository,
        private val processor: RefusjonskravBehandler,
        coroutineScope: CoroutineScope,
        freq: Duration
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

    override fun doJob() {
        mutualLock.lock()
        db.getByStatus(RefusjonskravStatus.FEILET)
                .forEach {
                    processor.behandle(it)
                    if (shutdownSignalSent) {
                        return@forEach
                    }
                }
        mutualLock.unlock()
    }
}