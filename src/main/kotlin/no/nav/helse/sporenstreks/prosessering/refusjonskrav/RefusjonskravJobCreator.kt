package no.nav.helse.sporenstreks.prosessering.refusjonskrav

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.CoroutineScope
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.utils.RecurringJob
import no.nav.helse.sporenstreks.db.RefusjonskravRepository
import no.nav.helse.sporenstreks.domene.RefusjonskravStatus

const val PROCESS_LIMIT = 250

class RefusjonskravJobCreator(
        private val refusjonskravRepo: RefusjonskravRepository,
        private val bakgrunnsjobbRepo: BakgrunnsjobbRepository,
        val om: ObjectMapper,
        coroutineScope: CoroutineScope,
        waitMillisWhenEmptyQueue: Long = (30 * 1000L)
) : RecurringJob(coroutineScope, waitMillisWhenEmptyQueue) {


    override fun doJob() {
        opprettRefusjonskravJobForStatus(RefusjonskravStatus.FEILET)
        opprettRefusjonskravJobForStatus(RefusjonskravStatus.MOTTATT)
    }

    private fun opprettRefusjonskravJobForStatus(status: RefusjonskravStatus) {
        refusjonskravRepo.getByStatus(status, PROCESS_LIMIT)
                .forEach {
                    bakgrunnsjobbRepo.save(
                            Bakgrunnsjobb(
                                    type = RefusjonskravBehandler.JOBB_TYPE,
                                    data = om.writeValueAsString(RefusjonskravJobData(
                                            it.id
                                    )),
                                    maksAntallForsoek = 14
                            )
                    )
                    it.status = RefusjonskravStatus.JOBB
                    refusjonskravRepo.update(it)
                    if (!isRunning) {
                        return@forEach
                    }
                }
    }
}
