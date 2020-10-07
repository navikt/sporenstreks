package no.nav.helse.sporenstreks.integrasjon

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.*
import no.nav.helse.TestData
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OppgaveKlient
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OpprettOppgaveRequest
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OpprettOppgaveResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.IOException

internal class OppgaveServiceTest {

    val oppgaveKlientMock = mockk<OppgaveKlient>()
    val objectMapperMock = mockk<ObjectMapper>()

    val oppgaveService = OppgaveService(oppgaveKlientMock, objectMapperMock)

    private val mockJson = "beskrivelse-json"
    private val mockAktørId = "aktør-id"
    private val mockJoarkRef = "joark-ref"
    private val mockOppgaveId = 4329

    @Test
    fun `Mapper krav og sender oppgaveopprettelse`() {
        val mappedRequest = slot<OpprettOppgaveRequest>()


        every { objectMapperMock.writeValueAsString(any())} returns mockJson
        coEvery { oppgaveKlientMock.opprettOppgave(capture(mappedRequest), any()) } returns OpprettOppgaveResponse(mockOppgaveId)

        val result = oppgaveService.opprettOppgave(
                TestData.gyldigKrav,
                mockJoarkRef,
                mockAktørId,
                "call-id"
        )

        coVerify(exactly = 1) { oppgaveKlientMock.opprettOppgave(any(), any()) }
        verify(exactly = 1) { objectMapperMock.writeValueAsString(any()) }

        assertThat(result).isEqualTo("$mockOppgaveId")

        assertThat(mappedRequest.isCaptured).isTrue()
        assertThat(mappedRequest.captured.aktoerId).isEqualTo(mockAktørId)
        assertThat(mappedRequest.captured.journalpostId).isEqualTo(mockJoarkRef)
        assertThat(mappedRequest.captured.beskrivelse).isEqualTo(mockJson)
    }

    @Test
    fun `Alle feil propagerer opp`() {
        every { objectMapperMock.writeValueAsString(any()) } throws IOException()
        assertThrows<IOException> {
            oppgaveService.opprettOppgave(
                    TestData.gyldigKrav,
                    mockJoarkRef,
                    mockAktørId,
                    "call-id"
            )
        }
    }


    }