package no.nav.helse.sporenstreks.prosessering.kvittering

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.CoroutineScope
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.utils.RecurringJob
import no.nav.helse.sporenstreks.db.KvitteringRepository
import no.nav.helse.sporenstreks.kvittering.KvitteringStatus
import javax.sql.DataSource

const val KVITTERINGER_TO_PROCESS_LIMIT = 250


/**
 * Denne klassen er unødvendig når alle eksisterende kvitteringer er flyttet over
 */
class KvitteringJobCreator(
        private val ds: DataSource,
        val kvitteringRepo: KvitteringRepository,
        val bakgrunnsjobbRepo: BakgrunnsjobbRepository,
        val om: ObjectMapper,
        coroutineScope: CoroutineScope,
        waitMillisWhenEmptyQueue: Long = (30 * 1000L)
) : RecurringJob(coroutineScope, waitMillisWhenEmptyQueue) {

    //val kvitteringRepo: KvitteringRepository = PostgresKvitteringRepository(ds, om)
    //val bakgrunnsjobbRepo: BakgrunnsjobbRepository = PostgresBakgrunnsjobbRepository(ds)


    override fun doJob() {
        opprettJobberForStatus(KvitteringStatus.FEILET)
        opprettJobberForStatus(KvitteringStatus.OPPRETTET)
    }

    private fun opprettJobberForStatus(kvitteringStatus: KvitteringStatus) {
        kvitteringRepo.getByStatus(kvitteringStatus, KVITTERINGER_TO_PROCESS_LIMIT)
                .forEach {
                    ds.connection.use { con ->
                        con.autoCommit = false
                        bakgrunnsjobbRepo.save(
                                Bakgrunnsjobb(
                                        type = KvitteringProcessor.JOBB_TYPE,
                                        data = om.writeValueAsString(KvitteringJobData(it.id)),
                                        maksAntallForsoek = 14
                                ), con)
                        it.status = KvitteringStatus.JOBB
                        kvitteringRepo.update(it, con)
                        con.commit()
                    }
                    if (!isRunning) {
                        return@forEach
                    }
                }
    }
}
