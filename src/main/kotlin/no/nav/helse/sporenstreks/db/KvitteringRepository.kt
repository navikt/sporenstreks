package no.nav.helse.sporenstreks.db

import no.nav.helse.sporenstreks.kvittering.Kvittering
import no.nav.helse.sporenstreks.kvittering.KvitteringStatus
import java.sql.Connection
import java.util.*

interface KvitteringRepository {

    fun insert(kvittering: Kvittering): Kvittering
    fun insert(kvittering: Kvittering, connection: Connection): Kvittering
    fun getByStatus(status: KvitteringStatus, limit: Int): List<Kvittering>
    fun getById(id: UUID): Kvittering?
    fun delete(id: UUID): Int
    fun update(kvittering: Kvittering)

}