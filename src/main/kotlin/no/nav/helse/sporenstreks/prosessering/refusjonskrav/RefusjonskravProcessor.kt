package no.nav.helse.sporenstreks.prosessering.refusjonskrav

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbProsesserer
import no.nav.helse.sporenstreks.db.RefusjonskravRepository
import no.nav.helse.sporenstreks.domene.Refusjonskrav
import no.nav.helse.sporenstreks.domene.RefusjonskravStatus
import no.nav.helse.sporenstreks.integrasjon.JoarkService
import no.nav.helse.sporenstreks.integrasjon.OppgaveService
import no.nav.helse.sporenstreks.integrasjon.rest.aktor.AktorConsumer
import no.nav.helse.sporenstreks.metrics.JOURNALFOERING_COUNTER
import no.nav.helse.sporenstreks.metrics.KRAV_TIME
import no.nav.helse.sporenstreks.metrics.OPPGAVE_COUNTER
import no.nav.helse.sporenstreks.utils.MDCOperations
import no.nav.helse.sporenstreks.utils.withMDC
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

class RefusjonskravProcessor(
    val joarkService: JoarkService,
    val oppgaveService: OppgaveService,
    val repository: RefusjonskravRepository,
    val aktorConsumer: AktorConsumer,
    val om: ObjectMapper
) : BakgrunnsjobbProsesserer {

    override val type: String = JOBB_TYPE

    val logger = LoggerFactory.getLogger(RefusjonskravProcessor::class.java)

    override fun nesteForsoek(forsoek: Int, forrigeForsoek: LocalDateTime): LocalDateTime {
        return forrigeForsoek.plusHours(2)
    }

    override fun prosesser(jobb: Bakgrunnsjobb) {
        val refusjonskravJobbData = om.readValue(jobb.data, RefusjonskravJobData::class.java)
        repository.getById(refusjonskravJobbData.kravId)?.let { behandle(it) }
    }

    fun behandle(refusjonskrav: Refusjonskrav) {
        val callId = MDCOperations.generateCallId()
        withMDC(mapOf("x_call_id" to callId)) {
            behandle(refusjonskrav, callId)
        }
    }

    private fun behandle(refusjonskrav: Refusjonskrav, callId: String) {
        if (refusjonskrav.status == RefusjonskravStatus.SENDT_TIL_BEHANDLING) {
            return
        }
        val timer = KRAV_TIME.startTimer()
        log.info("Prosesserer: ${refusjonskrav.id}")
        try {

            if (refusjonskrav.joarkReferanse.isNullOrBlank()) {
                refusjonskrav.joarkReferanse = joarkService.journalfør(refusjonskrav, callId)
                JOURNALFOERING_COUNTER.inc()
            }

            if (refusjonskrav.oppgaveId.isNullOrBlank()) {

                val aktørId = aktorConsumer.getAktorId(refusjonskrav.identitetsnummer, callId)

                refusjonskrav.oppgaveId = oppgaveService.opprettOppgave(
                    refusjonskrav,
                    refusjonskrav.joarkReferanse!!,
                    aktørId,
                    callId
                )
                OPPGAVE_COUNTER.inc()
            }
            refusjonskrav.status = RefusjonskravStatus.SENDT_TIL_BEHANDLING
        } finally {
            try {
                timer.close()
                repository.update(refusjonskrav)
            } catch (t: Throwable) {
                logger.error("Feilet i lagring av ${refusjonskrav.id} med  joarkRef: ${refusjonskrav.joarkReferanse} oppgaveId ${refusjonskrav.oppgaveId} ")
                throw t
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(RefusjonskravProcessor::class.java)
        val JOBB_TYPE = "refusjonskrav"
    }
}

data class RefusjonskravJobData(
    val kravId: UUID
)
