package no.nav.helse.sporenstreks.service

import no.nav.helse.sporenstreks.domene.Refusjonskrav

interface RefusjonskravService {

    fun saveKravWithKvittering(krav: Refusjonskrav): Refusjonskrav
    fun saveKravListWithKvittering(kravList: List<Refusjonskrav>): List<Refusjonskrav>
    fun updateKravListWithKvittering(kravList: List<Refusjonskrav>)
    fun getAllForVirksomhet(virksomhetsnummer: String): List<Refusjonskrav>
    fun bulkInsert(kravListe: List<Refusjonskrav>): List<Int>
}