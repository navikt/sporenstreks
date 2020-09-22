package no.nav.helse.sporenstreks.service

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.PostgresBakgrunnsjobbRepository
import no.nav.helse.sporenstreks.db.KvitteringRepository
import no.nav.helse.sporenstreks.db.PostgresKvitteringRepository
import no.nav.helse.sporenstreks.db.PostgresRefusjonskravRepository
import no.nav.helse.sporenstreks.db.RefusjonskravRepository
import no.nav.helse.sporenstreks.domene.Refusjonskrav
import no.nav.helse.sporenstreks.domene.RefusjonskravStatus
import no.nav.helse.sporenstreks.kvittering.Kvittering
import no.nav.helse.sporenstreks.kvittering.KvitteringStatus
import no.nav.helse.sporenstreks.prosessering.kvittering.KvitteringJobData
import no.nav.helse.sporenstreks.prosessering.kvittering.KvitteringProcessor
import no.nav.helse.sporenstreks.prosessering.refusjonskrav.RefusjonskravBehandler
import no.nav.helse.sporenstreks.prosessering.refusjonskrav.RefusjonskravJobData
import org.slf4j.LoggerFactory
import java.sql.SQLException
import java.time.LocalDateTime
import javax.sql.DataSource

class PostgresRefusjonskravService(
        val ds: DataSource,
        val mapper: ObjectMapper
) : RefusjonskravService {

    private val logger = LoggerFactory.getLogger(PostgresRefusjonskravService::class.java)

    val refusjonskravRepository: RefusjonskravRepository = PostgresRefusjonskravRepository(ds, mapper)
    val kvitteringRepository: KvitteringRepository = PostgresKvitteringRepository(ds, mapper)
    val bakgrunnsjobbRepository: BakgrunnsjobbRepository = PostgresBakgrunnsjobbRepository(ds)

    override fun saveKravWithKvittering(krav: Refusjonskrav): Refusjonskrav {
        ds.connection.use {
            it.autoCommit = false

            val kvittering = Kvittering(
                    virksomhetsnummer = krav.virksomhetsnummer,
                    refusjonsListe = listOf(krav),
                    tidspunkt = LocalDateTime.now(),
                    status = KvitteringStatus.JOBB
            )
            val savedKvittering = kvitteringRepository.insert(kvittering, it)
            krav.kvitteringId = savedKvittering.id
            krav.status = RefusjonskravStatus.JOBB
            val saved = refusjonskravRepository.insert(krav, it)
            bakgrunnsjobbRepository.save(
                    Bakgrunnsjobb(
                            type = KvitteringProcessor.JOBB_TYPE,
                            data = mapper.writeValueAsString(KvitteringJobData(savedKvittering.id)),
                            maksAntallForsoek = 14
                    ), it)
            bakgrunnsjobbRepository.save(
                    Bakgrunnsjobb(
                            type = RefusjonskravBehandler.JOBB_TYPE,
                            data = mapper.writeValueAsString(RefusjonskravJobData(
                                    saved.id
                            )),
                            maksAntallForsoek = 14
                    )
            )
            it.commit()
            return saved
        }
    }

    override fun saveKravListWithKvittering(kravListeMedIndex: Map<Int, Refusjonskrav>): Map<Int, Refusjonskrav> {
        //Alle innsendingene må være på samme virksomhet
        ds.connection.use { con ->
            con.autoCommit = false
            val kvittering = Kvittering(
                    virksomhetsnummer = kravListeMedIndex.values.first().virksomhetsnummer,
                    refusjonsListe = kravListeMedIndex.values.toList(),
                    tidspunkt = LocalDateTime.now()
            )
            val savedKvittering = kvitteringRepository.insert(kvittering, con)
            val savedMap = mutableMapOf<Int, Refusjonskrav>()
            kravListeMedIndex.forEach {
                it.value.kvitteringId = savedKvittering.id
                savedMap[it.key] = refusjonskravRepository.insert(it.value, con)
            }
            con.commit()
            return savedMap
        }

    }


    override fun getAllForVirksomhet(virksomhetsnummer: String): List<Refusjonskrav> {
        return refusjonskravRepository.getAllForVirksomhet(virksomhetsnummer)
    }

    override fun bulkInsert(kravListe: List<Refusjonskrav>): List<Int> {
        ds.connection.use { con ->
            con.autoCommit = false
            try {
                val resultList = mutableListOf<Int>()
                kravListe.groupBy {
                    it.virksomhetsnummer
                }.forEach {
                    val savedKvittering = kvitteringRepository.insert(
                            Kvittering(virksomhetsnummer = it.key,
                                    refusjonsListe = it.value,
                                    tidspunkt = LocalDateTime.now()), con)
                    it.value.forEach { krav ->
                        krav.kvitteringId = savedKvittering.id
                    }
                    resultList.addAll(refusjonskravRepository.bulkInsert(it.value))
                }
                con.commit()
                return resultList
            } catch (e: SQLException) {
                logger.error("Ruller tilbake bulkinnsetting")
                try {
                    con.rollback()
                } catch (ex: Exception) {
                    logger.error("Klarte ikke rulle tilbake bulkinnsettingen", ex)
                }
                throw e
            }

        }
    }
}
