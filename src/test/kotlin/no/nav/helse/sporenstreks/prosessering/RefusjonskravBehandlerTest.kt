package no.nav.helse.sporenstreks.prosessering

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.sporenstreks.db.PostgresRefusjonskravRepository
import no.nav.helse.sporenstreks.domene.Refusjonskrav
import no.nav.helse.sporenstreks.domene.RefusjonskravStatus
import no.nav.helse.sporenstreks.integrasjon.JoarkService
import no.nav.helse.sporenstreks.integrasjon.OppgaveService
import no.nav.helse.sporenstreks.integrasjon.rest.aktor.AktorConsumer
import no.nav.helse.sporenstreks.utils.MDCOperations
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException

class RefusjonskravBehandlerTest {

    val joarkMock = mockk<JoarkService>(relaxed = true)
    val oppgaveMock = mockk<OppgaveService>(relaxed = true)
    val repositoryMock = mockk<PostgresRefusjonskravRepository>(relaxed = true)
    val aktorConsumerMock = mockk<AktorConsumer>(relaxed = true)
    val refusjonskravBehandler = RefusjonskravBehandler(joarkMock, oppgaveMock, repositoryMock, aktorConsumerMock)
    lateinit var refusjonskrav: Refusjonskrav

    @BeforeEach
    fun setup() {
        refusjonskrav = Refusjonskrav(
                opprettetAv = "Lu Bu",
                identitetsnummer = "123",
                virksomhetsnummer = "213",
                perioder = emptySet(),
                status = RefusjonskravStatus.FEILET
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

        every { oppgaveMock.opprettOppgave(refusjonskrav, joarkref, aktørId, MDCOperations.generateCallId()) } returns opgref

        refusjonskravBehandler.behandle(refusjonskrav)

        assertThat(refusjonskrav.status).isEqualTo(RefusjonskravStatus.SENDT_TIL_BEHANDLING)
        assertThat(refusjonskrav.joarkReferanse).isEqualTo(joarkref)
        assertThat(refusjonskrav.oppgaveId).isEqualTo(opgref)

        verify(exactly = 1) { joarkMock.journalfør(any(), any()) }
        verify(exactly = 1) { oppgaveMock.opprettOppgave(any(), any(), any(), MDCOperations.generateCallId()) }
        verify(exactly = 1) { repositoryMock.update(refusjonskrav) }
    }


    @Test
    fun `Ved feil skal kravet lagres med feilstatus og joarkref om det finnes`() {
        val joarkref = "joarkref"
        val aktørId = "aktørId"

        every { joarkMock.journalfør(refusjonskrav, any()) } returns joarkref

        every { oppgaveMock.opprettOppgave(refusjonskrav, joarkref, aktørId, MDCOperations.generateCallId()) } throws IOException()

        refusjonskravBehandler.behandle(refusjonskrav)

        assertThat(refusjonskrav.status).isEqualTo(RefusjonskravStatus.FEILET)
        assertThat(refusjonskrav.joarkReferanse).isEqualTo(joarkref)
        assertThat(refusjonskrav.oppgaveId).isNull()

        verify(exactly = 1) { joarkMock.journalfør(any(), any()) }
        verify(exactly = 1) { oppgaveMock.opprettOppgave(any(), any(), any(), MDCOperations.generateCallId()) }
        verify(exactly = 1) { repositoryMock.update(refusjonskrav) }
    }
}