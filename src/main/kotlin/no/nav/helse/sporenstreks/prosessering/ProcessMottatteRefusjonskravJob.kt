package no.nav.helse.sporenstreks.prosessering

import kotlinx.coroutines.CoroutineScope
import no.nav.helse.sporenstreks.db.RefusjonskravRepository
import no.nav.helse.sporenstreks.domene.RefusjonskravStatus
import java.time.Duration
const val KRAV_TO_PROCESS_LIMIT = 1000
class ProcessMottatteRefusjonskravJob(
        private val db: RefusjonskravRepository,
        private val processor: RefusjonskravBehandler,
        coroutineScope: CoroutineScope,
        freq: Duration
) : RecurringJob(coroutineScope, freq) {

    override fun doJob() {
        db.getByStatus(RefusjonskravStatus.MOTTATT, KRAV_TO_PROCESS_LIMIT)
                .forEach(processor::behandle)
    }
}