package no.nav.helse.sporenstreks.domene


import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.zaxxer.hikari.HikariDataSource
import io.mockk.*
import no.nav.helse.TestData
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OppgaveKlient
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OpprettOppgaveRequest
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OpprettOppgaveResponse
import no.nav.helse.sporenstreks.db.PostgresRefusjonskravRepository
import no.nav.helse.sporenstreks.db.createNonVaultHikariConfig
import no.nav.helse.sporenstreks.web.common
import no.nav.helse.sporenstreks.integrasjon.OppgaveService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.get
import org.koin.core.KoinComponent
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import java.time.LocalDate
import java.time.LocalDateTime

internal class RefusjonskravTest : KoinComponent { // burde disse testene ligge et annet sted?

    val oppgaveKlientMock = mockk<OppgaveKlient>()
    lateinit var oppgaveService : OppgaveService
    lateinit var repo : PostgresRefusjonskravRepository
    lateinit var om : ObjectMapper

    private val mockAktørId = "aktør-id"
    private val mockJoarkRef = "joark-ref"
    private val mockOppgaveId = 4329
    private val refusjonskrav = Refusjonskrav(
            opprettetAv = TestData.opprettetAv,
            identitetsnummer = TestData.validIdentitetsnummer,
            virksomhetsnummer = TestData.validOrgNr,
            perioder = setOf(Arbeidsgiverperiode(
                    LocalDate.of(2020, 4, 1),
                    LocalDate.of(2020, 4, 5),
                    2,
                    4500800.50
            )),
            opprettet = LocalDateTime.now(),
            status = RefusjonskravStatus.MOTTATT,
            referansenummer = 12345
    )

    @BeforeEach
    internal fun setUp() {
        startKoin {
            loadKoinModules(common)
            om = koin.get()
            oppgaveService = OppgaveService(oppgaveKlientMock, get())
            repo = PostgresRefusjonskravRepository(HikariDataSource(createNonVaultHikariConfig()), get())
        }
    }

    @AfterEach
    internal fun tearDown() {
        stopKoin()
    }


    @Test
    fun `Ignorerer opprettetAv ved serialisering til oppgave`() {
        val mappedRequest = slot<OpprettOppgaveRequest>()

        coEvery { oppgaveKlientMock.opprettOppgave(capture(mappedRequest), any()) } returns OpprettOppgaveResponse(mockOppgaveId)

        oppgaveService.opprettOppgave(
                refusjonskrav,
                mockJoarkRef,
                mockAktørId,
                "call-id"
        )

        val deserializedOppgaveRefusjonskrav = om.readValue<RefusjonskravForOppgave>(mappedRequest.captured.beskrivelse ?: "")

        assertThat(mappedRequest.isCaptured).isTrue()
        assertThat(deserializedOppgaveRefusjonskrav).isNotNull()
        assertThat(deserializedOppgaveRefusjonskrav.identitetsnummer).isEqualTo(TestData.validIdentitetsnummer)
    }

    @Test
    fun `Ignorerer ikke opprettetAv ved serialisering til database`() {
        val savedKrav =  repo.insert(refusjonskrav)

        repo.getById(savedKrav.id)

        assertThat(savedKrav).isNotNull()
        assertThat(savedKrav.opprettetAv).isEqualTo(TestData.opprettetAv)
    }


}