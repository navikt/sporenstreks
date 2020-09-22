package no.nav.helse.sporenstreks.prosessering

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.sporenstreks.db.PostgresRefusjonskravRepository
import no.nav.helse.sporenstreks.domene.Refusjonskrav
import no.nav.helse.sporenstreks.domene.RefusjonskravStatus
import no.nav.helse.sporenstreks.integrasjon.JoarkService
import no.nav.helse.sporenstreks.integrasjon.OppgaveService
import no.nav.helse.sporenstreks.integrasjon.rest.aktor.AktorConsumerImpl
import no.nav.helse.sporenstreks.prosessering.refusjonskrav.RefusjonskravBehandler
import no.nav.helse.sporenstreks.utils.MDCOperations
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.IOException

class RefusjonskravBehandlerTest {

    val joarkMock = mockk<JoarkService>(relaxed = true)
    val oppgaveMock = mockk<OppgaveService>(relaxed = true)
    val repositoryMock = mockk<PostgresRefusjonskravRepository>(relaxed = true)
    val aktorConsumerMock = mockk<AktorConsumerImpl>(relaxed = true)
    val refusjonskravBehandler = RefusjonskravBehandler(joarkMock, oppgaveMock, repositoryMock, aktorConsumerMock, ObjectMapper()) //TODO
    lateinit var refusjonskrav: Refusjonskrav

    @BeforeEach
    fun setup() {
        refusjonskrav = Refusjonskrav(
                identitetsnummer = "123",
                virksomhetsnummer = "213",
                perioder = emptySet(),
                status = RefusjonskravStatus.JOBB
        )
    }


    @Test
    fun `skal ikke journalføre når det allerede foreligger en journalpostId `() {
        refusjonskrav.joarkReferanse = "joark"
        refusjonskravBehandler.behandle(refusjonskrav)
        verify(exactly = 0) { joarkMock.journalfør(any(), MDCOperations.generateCallId()) }
    }

    @Test
    fun `skal ikke lage oppgave når det allerede foreligger en oppgaveId `() {
        refusjonskrav.oppgaveId = "ppggssv"
        refusjonskravBehandler.behandle(refusjonskrav)
        verify(exactly = 0) { oppgaveMock.opprettOppgave(any(), any(), any(), MDCOperations.generateCallId()) }
    }

    @Test
    fun `skal journalføre, opprette oppgave og oppdatere kravet i databasen`() {
        val joarkref = "joarkref"
        val opgref = "oppgaveref"
        val aktørId = "aktørId"

        every { joarkMock.journalfør(refusjonskrav, any()) } returns joarkref
        every { aktorConsumerMock.getAktorId(any(), any()) } returns aktørId

        every { oppgaveMock.opprettOppgave(refusjonskrav, joarkref, aktørId, any()) } returns opgref

        refusjonskravBehandler.behandle(refusjonskrav)

        assertThat(refusjonskrav.status).isEqualTo(RefusjonskravStatus.SENDT_TIL_BEHANDLING)
        assertThat(refusjonskrav.joarkReferanse).isEqualTo(joarkref)
        assertThat(refusjonskrav.oppgaveId).isEqualTo(opgref)

        verify(exactly = 1) { joarkMock.journalfør(any(), any()) }
        verify(exactly = 1) { oppgaveMock.opprettOppgave(any(), any(), any(), any()) }
        verify(exactly = 1) { repositoryMock.update(refusjonskrav) }
    }


    @Test
    fun `Ved feil skal kravet fortsatt ha status JOBB og joarkref om det finnes  og kaste exception oppover`() {
        val joarkref = "joarkref"
        val aktørId = "aktørId"

        every { joarkMock.journalfør(refusjonskrav, any()) } returns joarkref
        every { aktorConsumerMock.getAktorId(any(), any()) } returns aktørId

        every { oppgaveMock.opprettOppgave(refusjonskrav, joarkref, aktørId, any()) } throws IOException()

        assertThrows<IOException> { refusjonskravBehandler.behandle(refusjonskrav) }

        assertThat(refusjonskrav.status).isEqualTo(RefusjonskravStatus.JOBB)
        assertThat(refusjonskrav.joarkReferanse).isEqualTo(joarkref)
        assertThat(refusjonskrav.oppgaveId).isNull()

        verify(exactly = 1) { joarkMock.journalfør(any(), any()) }
        verify(exactly = 1) { oppgaveMock.opprettOppgave(any(), any(), any(), any()) }
        verify(exactly = 1) { repositoryMock.update(refusjonskrav) }
    }
}
