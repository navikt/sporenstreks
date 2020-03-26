package no.nav.helse.sporenstreks.prosessering

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.sporenstreks.db.PostgresRefusjonskravRepository
import no.nav.helse.sporenstreks.domene.Refusjonskrav
import no.nav.helse.sporenstreks.integrasjon.JoarkService
import no.nav.helse.sporenstreks.integrasjon.OppgaveService
import org.junit.Test

class RefusjonskravBehandlerTest {

    val joarkMock = mockk<JoarkService>(relaxed = true)
    val oppgaveMock = mockk<OppgaveService>(relaxed = true)
    val repositoryMock = mockk<PostgresRefusjonskravRepository>(relaxed = true)


    val refusjonskravBehandler = RefusjonskravBehandler(joarkMock, oppgaveMock, repositoryMock)

    @Test
    fun `skal ikke journalføre når det allerede foreligger en journalpostId `() {
        refusjonskravBehandler.behandle(Refusjonskrav(
                opprettetAv = "Lu Bu",
                identitetsnummer = "123",
                virksomhetsnummer = "213",
                perioder = emptySet(),
                joarkReferanse = "ABC"
        ))
        verify(exactly = 0) { joarkMock.journalfør(any()) }
    }


    @Test
    fun `skal journalføre når det ikke foreligger en journalpostId `() {
        refusjonskravBehandler.behandle(Refusjonskrav(
                opprettetAv = "Lu Bu",
                identitetsnummer = "123",
                virksomhetsnummer = "213",
                perioder = emptySet()
        ))
        verify(exactly = 1) { joarkMock.journalfør(any()) }
    }


}