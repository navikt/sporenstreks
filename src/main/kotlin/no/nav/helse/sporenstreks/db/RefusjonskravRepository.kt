package no.nav.helse.sporenstreks.db

import no.nav.helse.sporenstreks.domene.Refusjonskrav
import no.nav.helse.sporenstreks.domene.RefusjonskravStatus
import java.sql.Connection
import java.util.*

interface RefusjonskravRepository {
    fun getAllForVirksomhet(virksomhetsnummer: String): List<Refusjonskrav>
    fun insert(refusjonskrav: Refusjonskrav): Refusjonskrav
    fun insert(refusjonskrav: Refusjonskrav, connection: Connection): Refusjonskrav
    fun getExistingRefusjonskrav(identitetsnummer: String, virksomhetsnummer: String): List<Refusjonskrav>
    fun delete(id: UUID): Int
    fun getById(id: UUID): Refusjonskrav?
    fun getByStatus(status: RefusjonskravStatus, limit: Int): List<Refusjonskrav>
    fun getByIkkeIndeksertInflux(limit: Int): List<Refusjonskrav>
    fun update(krav: Refusjonskrav)
    fun update(krav: Refusjonskrav, connection: Connection)
    fun bulkInsert(kravListe: List<Refusjonskrav>): List<Int>
    fun bulkInsert(kravListe: List<Refusjonskrav>, connection: Connection): List<Int>
    fun getAllForVirksomhetWithoutKvittering(virksomhetsnummer: String): List<Refusjonskrav>
    fun getRandomVirksomhetWithoutKvittering(): String?
}
