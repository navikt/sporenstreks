package no.nav.helse.sporenstreks.integrasjon

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.*
import no.nav.helse.TestData
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OppgaveKlient
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OpprettOppgaveRequest
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OpprettOppgaveResponse
import no.nav.helse.sporenstreks.domene.Arbeidsgiverperiode
import no.nav.helse.sporenstreks.domene.Refusjonskrav
import no.nav.helse.sporenstreks.domene.RefusjonskravForOppgave
import no.nav.helse.sporenstreks.domene.RefusjonskravStatus
import no.nav.helse.sporenstreks.web.common
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime

internal class OppgaveServiceTest : KoinTest{

    val oppgaveKlientMock = mockk<OppgaveKlient>()
    val objectMapperMock = mockk<ObjectMapper>()

    val oppgaveService = OppgaveService(oppgaveKlientMock, objectMapperMock)
    lateinit var om : ObjectMapper

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
    fun `Ignorerer opprettetAv ved serialisering til oppgave`() {
        startKoin {
            modules(
                    module {
                        loadKoinModules(common)
                    }
            )
        }

        val mappedRequest = slot<OpprettOppgaveRequest>()
        coEvery { oppgaveKlientMock.opprettOppgave(capture(mappedRequest), any()) } returns OpprettOppgaveResponse(mockOppgaveId)

        om = get<ObjectMapper>()

        val oppgaveServiceMedMapper = OppgaveService(oppgaveKlientMock, om)

        oppgaveServiceMedMapper.opprettOppgave(
                TestData.gyldigKrav,
                mockJoarkRef,
                mockAktørId,
                "call-id"
        )

        val deserializedOppgaveRefusjonskrav = om.readValue<RefusjonskravForOppgave>(mappedRequest.captured.beskrivelse ?: "")

        assertThat(mappedRequest.isCaptured).isTrue()
        assertThat(deserializedOppgaveRefusjonskrav).isNotNull()

        stopKoin()
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