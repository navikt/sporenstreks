package no.nav.helse.sporenstreks.prosessering

import no.nav.helse.sporenstreks.db.PostgresRefusjonskravRepository
import no.nav.helse.sporenstreks.db.RefusjonskravRepository
import no.nav.helse.sporenstreks.domene.Refusjonskrav
import no.nav.helse.sporenstreks.domene.RefusjonskravStatus
import no.nav.helse.sporenstreks.integrasjon.JoarkService
import no.nav.helse.sporenstreks.integrasjon.OppgaveService
import no.nav.helse.sporenstreks.integrasjon.rest.aktor.AktorConsumer
import no.nav.helse.sporenstreks.metrics.FEIL_COUNTER
import no.nav.helse.sporenstreks.utils.MDCOperations
import org.slf4j.LoggerFactory

class RefusjonskravBehandler(val joarkService: JoarkService,
                             val oppgaveService: OppgaveService,
                             val repository: RefusjonskravRepository,
                             val aktorConsumer: AktorConsumer) {

    val logger = LoggerFactory.getLogger(RefusjonskravBehandler::class.java)

    fun behandle(refusjonskrav: Refusjonskrav) {
        val callId = MDCOperations.generateCallId()
        if (refusjonskrav.status == RefusjonskravStatus.SENDT_TIL_BEHANDLING) {
            return
        }

        try {
            if (refusjonskrav.joarkReferanse.isNullOrBlank()) {
                refusjonskrav.joarkReferanse = joarkService.journalfør(refusjonskrav, callId)
            }

            if (refusjonskrav.oppgaveId.isNullOrBlank()) {

                val aktørId = aktorConsumer.getAktorId(refusjonskrav.identitetsnummer, callId)

                refusjonskrav.oppgaveId = oppgaveService.opprettOppgave(
                        refusjonskrav,
                        refusjonskrav.joarkReferanse!!,
                        aktørId,
                        callId
                )
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
                repository.update(refusjonskrav)
            } catch (t: Throwable) {
                logger.error("Feilet i lagring av ${refusjonskrav.id} med  joarkRef: ${refusjonskrav.joarkReferanse} oppgaveId ${refusjonskrav.oppgaveId} ")
            }
        }
    }
}