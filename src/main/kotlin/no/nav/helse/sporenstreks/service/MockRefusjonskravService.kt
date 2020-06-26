package no.nav.helse.sporenstreks.service

import no.nav.helse.sporenstreks.db.RefusjonskravRepository
import no.nav.helse.sporenstreks.domene.Refusjonskrav

class MockRefusjonskravService(val refusjonskravRepo: RefusjonskravRepository) : RefusjonskravService {

    override fun saveKravWithKvittering(krav: Refusjonskrav): Refusjonskrav {
        return refusjonskravRepo.insert(krav)
    }

    override fun saveKravListWithKvittering(kravList: List<Refusjonskrav>): List<Refusjonskrav> {
        var savedList = mutableListOf<Refusjonskrav>()
        kravList.forEach {
            savedList.add(refusjonskravRepo.insert(it))
        }
        return savedList
    }

    override fun getAllForVirksomhet(virksomhetsnummer: String): List<Refusjonskrav> {
        return refusjonskravRepo.getAllForVirksomhet(virksomhetsnummer)
    }

    override fun bulkInsert(kravListe: List<Refusjonskrav>): List<Int> {
        return refusjonskravRepo.bulkInsert(kravListe)
    }

    override fun updateKravListWithKvittering(kravList: List<Refusjonskrav>) {
        TODO("Not yet implemented")
    }
}