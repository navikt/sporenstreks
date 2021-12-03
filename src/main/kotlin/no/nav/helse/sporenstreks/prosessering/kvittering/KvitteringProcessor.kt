package no.nav.helse.sporenstreks.prosessering.kvittering

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbProsesserer
import no.nav.helse.sporenstreks.db.KvitteringRepository
import no.nav.helse.sporenstreks.kvittering.KvitteringSender
import java.time.LocalDateTime
import java.util.*

class KvitteringProcessor(
    val kvitteringSender: KvitteringSender,
    val db: KvitteringRepository,
    val om: ObjectMapper
) : BakgrunnsjobbProsesserer {

    override val type = JOBB_TYPE

    companion object {
        val JOBB_TYPE = "kvittering"
    }

    override fun nesteForsoek(forsoek: Int, forrigeForsoek: LocalDateTime): LocalDateTime {
        return forrigeForsoek.plusHours(2)
    }

    override fun prosesser(jobb: Bakgrunnsjobb) {
        val kvitteringJobbData = om.readValue(jobb.data, KvitteringJobData::class.java)
        val kvittering = db.getById(kvitteringJobbData.kvitteringId)
        kvittering?.let { kvitteringSender.send(it) }
    }
}

data class KvitteringJobData(
    val kvitteringId: UUID
)
