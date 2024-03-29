package no.nav.helse.sporenstreks.db

import no.nav.helse.sporenstreks.domene.Refusjonskrav
import no.nav.helse.sporenstreks.domene.RefusjonskravStatus
import java.sql.Connection
import java.util.*

class MockRefusjonskravRepo : RefusjonskravRepository {

    private val refusjonskravListe = mutableListOf<Refusjonskrav>()

    override fun getAllForVirksomhet(virksomhetsnummer: String): List<Refusjonskrav> {
        return refusjonskravListe.filter {
            it.virksomhetsnummer == virksomhetsnummer
        }
    }

    override fun insert(refusjonskrav: Refusjonskrav, connection: Connection): Refusjonskrav {
        return insert(refusjonskrav)
    }

    override fun update(krav: Refusjonskrav, connection: Connection) {
        update(krav)
    }

    override fun bulkInsert(kravListe: List<Refusjonskrav>, connection: Connection): List<Int> {
        kravListe.forEach { insert(it) }
        return emptyList()
    }

    override fun getAllForVirksomhetWithoutKvittering(virksomhetsnummer: String): List<Refusjonskrav> {
        TODO("Not yet implemented")
    }

    override fun getRandomVirksomhetWithoutKvittering(): String? {
        TODO("Not yet implemented")
    }

    override fun insert(refusjonskrav: Refusjonskrav): Refusjonskrav {
        refusjonskravListe.add(refusjonskrav)
        return refusjonskrav
    }

    override fun getExistingRefusjonskrav(identitetsnummer: String, virksomhetsnummer: String): List<Refusjonskrav> {
        return refusjonskravListe.filter {
            it.virksomhetsnummer == virksomhetsnummer && it.identitetsnummer == identitetsnummer
        }
    }

    override fun delete(id: UUID): Int {
        return if (refusjonskravListe.removeIf {
            it.id == id
        }
        ) 1 else 0
    }

    override fun getById(id: UUID): Refusjonskrav {
        return refusjonskravListe.first {
            it.id == id
        }
    }

    override fun getByStatus(status: RefusjonskravStatus, limit: Int): List<Refusjonskrav> {
        return refusjonskravListe.filter {
            it.status == status
        }
    }

    override fun getByIkkeIndeksertInflux(limit: Int): List<Refusjonskrav> {
        TODO("Not yet implemented")
    }

    override fun update(krav: Refusjonskrav) {
    }

    override fun bulkInsert(kravListe: List<Refusjonskrav>): List<Int> {
        return emptyList()
    }
}
