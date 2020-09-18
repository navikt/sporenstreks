package no.nav.helse.sporenstreks.prosessering.kvittering

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.CoroutineScope
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.utils.RecurringJob
import no.nav.helse.sporenstreks.db.KvitteringRepository
import no.nav.helse.sporenstreks.kvittering.KvitteringStatus

const val KVITTERINGER_TO_PROCESS_LIMIT = 250


class KvitteringJobCreator(
        private val kvitteringRepo: KvitteringRepository,
        private val bakgrunnsjobbRepo: BakgrunnsjobbRepository,
        val om: ObjectMapper,
        coroutineScope: CoroutineScope,
        waitMillisWhenEmptyQueue: Long = (30 * 1000L)
) : RecurringJob(coroutineScope, waitMillisWhenEmptyQueue) {

    override fun doJob() {
        opprettJobberForStatus(KvitteringStatus.FEILET)
        opprettJobberForStatus(KvitteringStatus.OPPRETTET)
    }

    private fun opprettJobberForStatus(kvitteringStatus: KvitteringStatus) {
        kvitteringRepo.getByStatus(kvitteringStatus, KVITTERINGER_TO_PROCESS_LIMIT)
                .forEach {
                    bakgrunnsjobbRepo.save(
                            Bakgrunnsjobb(
                                    type = KvitteringProcessor.JOBB_TYPE,
                                    data = om.writeValueAsString(KvitteringJobData(it.id)),
                                    maksAntallForsoek = 14
                            )
                    )
                    it.status = KvitteringStatus.JOBB
                    kvitteringRepo.update(it)
                    if (!isRunning) {
                        return@forEach
                    }
                }
    }
}
