package no.nav.helse.sporenstreks.service

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.sporenstreks.db.KvitteringRepository
import no.nav.helse.sporenstreks.db.PostgresKvitteringRepository
import no.nav.helse.sporenstreks.db.PostgresRefusjonskravRepository
import no.nav.helse.sporenstreks.db.RefusjonskravRepository
import no.nav.helse.sporenstreks.domene.Refusjonskrav
import no.nav.helse.sporenstreks.kvittering.Kvittering
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

    override fun saveKravWithKvittering(krav: Refusjonskrav): Refusjonskrav {
        ds.connection.use {
            it.autoCommit = false

            val kvittering = Kvittering(
                    virksomhetsnummer = krav.virksomhetsnummer,
                    refusjonsListe = listOf(krav),
                    tidspunkt = LocalDateTime.now()
            )
            val savedKvittering = kvitteringRepository.insert(kvittering, it)
            krav.kvitteringId = savedKvittering.id
            val saved = refusjonskravRepository.insert(krav, it)
            it.commit()
            return saved
        }
    }

    override fun saveKravListWithKvittering(kravList: List<Refusjonskrav>): List<Refusjonskrav> {
        //Alle innsendingene må være på samme virksomhet
        ds.connection.use { con ->
            con.autoCommit = false
            val kvittering = Kvittering(
                    virksomhetsnummer = kravList.first().virksomhetsnummer,
                    refusjonsListe = kravList,
                    tidspunkt = LocalDateTime.now()
            )
            val savedKvittering = kvitteringRepository.insert(kvittering, con)
            val savedList = mutableListOf<Refusjonskrav>()
            kravList.forEach {
                it.kvitteringId = savedKvittering.id
                val saved = refusjonskravRepository.insert(it, con)
                savedList.add(saved)
            }
            con.commit()
            return savedList
        }

    }

    override fun updateKravListWithKvittering(kravList: List<Refusjonskrav>) {
        //Alle innsendingene må være på samme virksomhet
        ds.connection.use { con ->
            con.autoCommit = false
            val kvittering = Kvittering(
                    virksomhetsnummer = kravList.first().virksomhetsnummer,
                    refusjonsListe = kravList.sorted(),
                    tidspunkt = LocalDateTime.now()
            )
            val savedKvittering = kvitteringRepository.insert(kvittering, con)
            kravList.forEach {
                it.kvitteringId = savedKvittering.id
                refusjonskravRepository.update(it, con)
            }
            con.commit()
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
                                    tidspunkt = LocalDateTime.now())
                            , con)
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
