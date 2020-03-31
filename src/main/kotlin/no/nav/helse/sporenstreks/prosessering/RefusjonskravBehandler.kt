package no.nav.helse.sporenstreks.prosessering

import no.nav.helse.sporenstreks.db.RefusjonskravRepository
import no.nav.helse.sporenstreks.domene.Refusjonskrav
import no.nav.helse.sporenstreks.domene.RefusjonskravStatus
import no.nav.helse.sporenstreks.integrasjon.JoarkService
import no.nav.helse.sporenstreks.integrasjon.OppgaveService
import no.nav.helse.sporenstreks.integrasjon.rest.aktor.AktorConsumer
import no.nav.helse.sporenstreks.metrics.FEIL_COUNTER
import no.nav.helse.sporenstreks.metrics.JOURNALFOERING_COUNTER
import no.nav.helse.sporenstreks.metrics.KRAV_TIME
import no.nav.helse.sporenstreks.metrics.OPPGAVE_COUNTER
import no.nav.helse.sporenstreks.utils.MDCOperations
import org.slf4j.LoggerFactory

class RefusjonskravBehandler(val joarkService: JoarkService,
                             val oppgaveService: OppgaveService,
                             val repository: RefusjonskravRepository,
                             val aktorConsumer: AktorConsumer) {

    val logger = LoggerFactory.getLogger(RefusjonskravBehandler::class.java)

    fun behandle(refusjonskrav: Refusjonskrav) {
        if (refusjonskrav.status == RefusjonskravStatus.SENDT_TIL_BEHANDLING) {
            return
        }
        val timer = KRAV_TIME.startTimer()
        val callId = MDCOperations.generateCallId()
        log.info("Bruker callID $callId")
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

            refusjonskrav.feilmelding = null
            refusjonskrav.status = RefusjonskravStatus.SENDT_TIL_BEHANDLING
        } catch (e: Exception) {
            refusjonskrav.status = RefusjonskravStatus.FEILET
            refusjonskrav.feilmelding = e.message
            logger.error("Feilet i behandlingen av ${refusjonskrav.id}", e)
            FEIL_COUNTER.inc()
        } finally {
            try {
                timer.close()
                repository.update(refusjonskrav)
            } catch (t: Throwable) {
                logger.error("Feilet i lagring av ${refusjonskrav.id} med  joarkRef: ${refusjonskrav.joarkReferanse} oppgaveId ${refusjonskrav.oppgaveId} ")
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(RefusjonskravBehandler::class.java)
    }
}