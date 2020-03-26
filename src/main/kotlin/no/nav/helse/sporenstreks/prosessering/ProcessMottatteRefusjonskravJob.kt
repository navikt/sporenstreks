package no.nav.helse.sporenstreks.prosessering

import kotlinx.coroutines.CoroutineScope
import no.nav.helse.sporenstreks.db.RefusjonskravRepository
import no.nav.helse.sporenstreks.domene.RefusjonskravStatus
import java.time.Duration

class ProcessMottatteRefusjonskravJob(
        private val db: RefusjonskravRepository,
        private val processor: RefusjonskravBehandler,
        coroutineScope: CoroutineScope
) : RecurringJob(coroutineScope, Duration.ofMinutes(5)) {

    override fun doJob() {
        db.getByStatus(RefusjonskravStatus.MOTTATT)
                .forEach(processor::behandle)
    }
}