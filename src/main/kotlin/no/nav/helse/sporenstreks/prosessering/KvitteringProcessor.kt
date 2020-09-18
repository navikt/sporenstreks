package no.nav.helse.sporenstreks.prosessering

import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbProsesserer
import java.time.LocalDateTime

class KvitteringProcessor : BakgrunnsjobbProsesserer(
        val leaderElectionConsumer: LeaderElectionConsumer
) {

    override fun nesteForsoek(forsoek: Int, forrigeForsoek: LocalDateTime): LocalDateTime {
        return LocalDateTime.now().plusHours(2)
    }

    override fun prosesser(jobbData: String) {

    }
}
