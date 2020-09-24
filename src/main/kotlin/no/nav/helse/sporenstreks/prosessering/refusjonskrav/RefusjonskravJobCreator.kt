package no.nav.helse.sporenstreks.prosessering.refusjonskrav

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.CoroutineScope
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.utils.RecurringJob
import no.nav.helse.sporenstreks.db.RefusjonskravRepository
import no.nav.helse.sporenstreks.domene.RefusjonskravStatus
import javax.sql.DataSource

const val PROCESS_LIMIT = 250

/**
 * Denne klassen er unødvendig når alle eksisterende refusjonskrav er flyttet over
 */
class RefusjonskravJobCreator(
        private val ds: DataSource,
        private val refusjonskravRepo: RefusjonskravRepository,
        private val bakgrunnsjobbRepo: BakgrunnsjobbRepository,
        val om: ObjectMapper,
        coroutineScope: CoroutineScope,
        waitMillisWhenEmptyQueue: Long = (30 * 1000L)
) : RecurringJob(coroutineScope, waitMillisWhenEmptyQueue) {

    override fun doJob() {
        opprettRefusjonskravJobForStatus(RefusjonskravStatus.MOTTATT)
        opprettRefusjonskravJobForStatus(RefusjonskravStatus.FEILET)
    }

    private fun opprettRefusjonskravJobForStatus(status: RefusjonskravStatus) {
        refusjonskravRepo.getByStatus(status, PROCESS_LIMIT)
                .forEach {
                    ds.connection.use { con ->
                        con.autoCommit = false
                        bakgrunnsjobbRepo.save(
                                Bakgrunnsjobb(
                                        type = RefusjonskravProcessor.JOBB_TYPE,
                                        data = om.writeValueAsString(RefusjonskravJobData(
                                                it.id
                                        )),
                                        maksAntallForsoek = 14
                                ), con)
                        it.status = RefusjonskravStatus.JOBB
                        refusjonskravRepo.update(it, con)
                        con.commit()
                    }
                    if (!isRunning) {
                        return@forEach
                    }
                }
    }
}
