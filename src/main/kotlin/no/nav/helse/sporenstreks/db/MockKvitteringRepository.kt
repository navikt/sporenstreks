package no.nav.helse.sporenstreks.db

import no.nav.helse.sporenstreks.kvittering.Kvittering
import no.nav.helse.sporenstreks.kvittering.KvitteringStatus
import java.sql.Connection
import java.util.*

class MockKvitteringRepository : KvitteringRepository {

    private val kvitteringListe = mutableListOf<Kvittering>()

    override fun insert(kvittering: Kvittering): Kvittering {
        kvitteringListe.add(kvittering)
        return kvittering
    }

    override fun insert(kvittering: Kvittering, connection: Connection): Kvittering {
        TODO("Not yet implemented")
    }

    override fun delete(id: UUID): Int {
        TODO("Not yet implemented")
    }

    override fun getByStatus(status: KvitteringStatus, limit: Int): List<Kvittering> {
        return kvitteringListe.filter {
            it.status == status
        }.take(limit)
    }

    override fun update(kvittering: Kvittering) {
        kvitteringListe.removeIf { it.id == kvittering.id }
        kvitteringListe.add(kvittering)
    }

    override fun getById(id: UUID): Kvittering? {
        return kvitteringListe.first {
            it.id == id
        }
    }
}