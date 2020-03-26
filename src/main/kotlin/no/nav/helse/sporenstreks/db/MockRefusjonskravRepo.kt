package no.nav.helse.sporenstreks.db

import no.nav.helse.sporenstreks.domene.Refusjonskrav
import java.util.*

class MockRefusjonskravRepo : RefusjonskravRepository {
    override fun getAllForVirksomhet(virksomhetsnummer: String): List<Refusjonskrav> {
        return emptyList()
    }

    override fun insert(refusjonskrav: Refusjonskrav): Refusjonskrav {
        return getById(UUID.randomUUID())
    }

    override fun getExistingRefusjonskrav(identitetsnummer: String, virksomhetsnummer: String): List<Refusjonskrav> {
        return emptyList()
    }

    override fun delete(id: UUID): Int {
        return 0
    }

    override fun getById(id: UUID): Refusjonskrav {
        return Refusjonskrav("", "", "", emptySet(), 0.0)
    }
}