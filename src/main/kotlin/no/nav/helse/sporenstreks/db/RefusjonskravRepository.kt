package no.nav.helse.sporenstreks.db

import no.nav.helse.sporenstreks.domene.Refusjonskrav
import java.util.*

interface RefusjonskravRepository {
    fun getAllForVirksomhet(virksomhetsnummer: String): List<Refusjonskrav>
    fun insert(refusjonskrav: Refusjonskrav)
    fun getExistingRefusjonskrav(identitetsnummer: String, virksomhetsnummer: String): List<Refusjonskrav>
    fun delete(id: UUID): Int
}