package no.nav.helse.sporenstreks.db

import no.nav.helse.sporenstreks.domene.Refusjonskrav
import no.nav.helse.sporenstreks.domene.RefusjonskravStatus
import java.util.*

interface RefusjonskravRepository {
    fun getAllForVirksomhet(virksomhetsnummer: String): List<Refusjonskrav>
    fun insert(refusjonskrav: Refusjonskrav): Refusjonskrav
    fun getExistingRefusjonskrav(identitetsnummer: String, virksomhetsnummer: String): List<Refusjonskrav>
    fun delete(id: UUID): Int
    fun getById(id: UUID): Refusjonskrav?
    fun getByStatus(status: RefusjonskravStatus): List<Refusjonskrav>
    fun update(krav: Refusjonskrav)
    fun bulkInsert(kravListe: List<Refusjonskrav>): List<Int>
}