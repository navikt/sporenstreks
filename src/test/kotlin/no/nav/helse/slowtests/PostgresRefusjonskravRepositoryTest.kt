package no.nav.helse.slowtests

import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.TestData
import no.nav.helse.sporenstreks.db.PostgresRefusjonskravRepository
import no.nav.helse.sporenstreks.db.createNonVaultHikariConfig
import no.nav.helse.sporenstreks.domene.Arbeidsgiverperiode
import no.nav.helse.sporenstreks.domene.Refusjonskrav
import no.nav.helse.sporenstreks.domene.RefusjonskravStatus
import no.nav.helse.sporenstreks.web.common
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.KoinComponent
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.get
import java.io.IOException
import java.time.LocalDate

internal class PostgresRefusjonskravRepositoryTest : KoinComponent {

    lateinit var repo: PostgresRefusjonskravRepository
    lateinit var refusjonskrav: Refusjonskrav

    @BeforeEach
    internal fun setUp() {
        startKoin {
            loadKoinModules(common)
        }
        repo = PostgresRefusjonskravRepository(HikariDataSource(createNonVaultHikariConfig()), get())
        refusjonskrav = repo.insert(
            Refusjonskrav(
                TestData.opprettetAv,
                TestData.notValidIdentitetsnummer,
                TestData.validOrgNr,
                setOf(
                    Arbeidsgiverperiode(
                        LocalDate.of(2020, 4, 1),
                        LocalDate.of(2020, 4, 6),
                        3, 1000.0
                    ),
                    Arbeidsgiverperiode(
                        LocalDate.of(2020, 4, 10),
                        LocalDate.of(2020, 4, 12),
                        3, 1000.0
                    )
                ),
                RefusjonskravStatus.MOTTATT,
                "oppgave-id-234234",
                "joark-ref-1232"
            )
        )
    }

    @AfterEach
    internal fun tearDown() {
        repo.delete(refusjonskrav.id)
        val rs = repo.getAllForVirksomhet(refusjonskrav.virksomhetsnummer)
        rs.forEach {
            repo.delete(it.id)
        }
        stopKoin()
    }

    @Test
    fun kan_finne_virksomhet_uten_kvittering() {
        val vnr = repo.getRandomVirksomhetWithoutKvittering()
        assertThat(vnr).isEqualTo(refusjonskrav.virksomhetsnummer)
    }

    @Test
    fun kan_finne_krav_uten_kvittering() {
        assertThat(repo.getAllForVirksomhetWithoutKvittering(refusjonskrav.virksomhetsnummer)).hasSize(1)
    }

    @Test
    fun kan_hente_ut_gammelt_refusjonskrav() {
        val refusjonskrav2 = repo.insert(
            GammeltRefusjonskrav(
                TestData.validIdentitetsnummer,
                TestData.notValidIdentitetsnummer,
                TestData.validOrgNr,
                setOf(
                    Arbeidsgiverperiode(
                        LocalDate.of(2020, 4, 1),
                        LocalDate.of(2020, 4, 6),
                        3, 1000.0
                    ),
                    Arbeidsgiverperiode(
                        LocalDate.of(2020, 4, 10),
                        LocalDate.of(2020, 4, 12),
                        3, 1000.0
                    )
                ),
                GammeltRefusjonskravStatus.MOTTATT,
                "oppgave-id-234234",
                "joark-ref-1232"
            )
        )
        repo.delete(refusjonskrav2.id)
    }

    fun PostgresRefusjonskravRepository.insert(refusjonskrav: GammeltRefusjonskrav): Refusjonskrav {
        val json = mapper.writeValueAsString(refusjonskrav)
        ds.connection.use {
            it.prepareStatement("INSERT INTO refusjonskrav (data) VALUES (?::json);").apply {
                setString(1, json)
            }.executeUpdate()
        }
        return getById(refusjonskrav.id)
            ?: throw IOException("Unable to read receipt for refusjonskrav with id ${refusjonskrav.id}")
    }

    @Test
    fun `Resultat fra Roundtrip til databasen skal være identisk med det som ble lagret`() {
        val rs = repo.getExistingRefusjonskrav(refusjonskrav.identitetsnummer, refusjonskrav.virksomhetsnummer)

        assertThat(rs.size).isEqualTo(1)
        assertThat(rs.first()).isEqualTo(refusjonskrav)
    }

    @Test
    fun `Kan hente fra virksomhetsnummer`() {
        val rs = repo.getAllForVirksomhet(refusjonskrav.virksomhetsnummer)

        assertThat(rs.size).isEqualTo(1)
        assertThat(rs.first()).isEqualTo(refusjonskrav)
    }

    @Test
    fun `Kan hente fra id`() {
        val krav = repo.getById(refusjonskrav.id)
        assertThat(krav).isEqualTo(refusjonskrav)
    }

    @Test
    fun `Kan hente fra status`() {
        val krav = repo.getByStatus(RefusjonskravStatus.MOTTATT, 10)
        assertThat(krav.size).isEqualTo(1)
        assertThat(krav.first()).isEqualTo(refusjonskrav)
    }

    @Test
    fun `Kan hente innenfor gitt limit fra status`() {

        for (i in 1..19) {
            repo.insert(
                Refusjonskrav(
                    TestData.opprettetAv,
                    TestData.notValidIdentitetsnummer,
                    TestData.validOrgNr,
                    setOf(
                        Arbeidsgiverperiode(
                            LocalDate.of(2020, 4, 1),
                            LocalDate.of(2020, 4, 6),
                            3, 1000.0
                        ),
                        Arbeidsgiverperiode(
                            LocalDate.of(2020, 4, 10),
                            LocalDate.of(2020, 4, 12),
                            3, 1000.0
                        )
                    ),
                    RefusjonskravStatus.MOTTATT,
                    "oppgave-id-234234",
                    "joark-ref-1232"
                )
            )
        }

        val tiKrav = repo.getByStatus(RefusjonskravStatus.MOTTATT, 10)
        val tjueKrav = repo.getByStatus(RefusjonskravStatus.MOTTATT, 20)

        assertThat(tiKrav).hasSize(10)
        assertThat(tjueKrav).hasSize(20)

        tjueKrav.forEach {
            repo.delete(it.id)
        }
    }

    @Test
    fun `Kan oppdatere krav`() {
        val res = repo.getByStatus(RefusjonskravStatus.MOTTATT, 10)
        val krav = res.first()

        krav.joarkReferanse = "dette er en test"
        krav.oppgaveId = "oppgaveId"
        krav.status = RefusjonskravStatus.SENDT_TIL_BEHANDLING

        repo.update(krav)

        val fromDb = repo.getById(krav.id)

        assertThat(krav).isEqualTo(fromDb)
    }

    @Test
    fun `Sletter et refusjonskrav`() {
        val deletedCount = repo.delete(refusjonskrav.id)
        assertThat(deletedCount).isEqualTo(1)
    }

    @Test
    fun `Finner bare krav som er indeksert`() {
        val ikkeIndeksertListe = repo.getByIkkeIndeksertInflux(100)
        assertThat(ikkeIndeksertListe).hasSize(1)
        val ikkeIndeksert = ikkeIndeksertListe.first()
        ikkeIndeksert.indeksertInflux = true
        repo.update(ikkeIndeksert)
        assertThat(repo.getByIkkeIndeksertInflux(100)).isEmpty()
        val krav2 = repo.insert(
            GammeltRefusjonskrav(
                TestData.validIdentitetsnummer,
                TestData.notValidIdentitetsnummer,
                TestData.validOrgNr,
                setOf(
                    Arbeidsgiverperiode(
                        LocalDate.of(2020, 4, 1),
                        LocalDate.of(2020, 4, 6),
                        3, 1000.0
                    ),
                    Arbeidsgiverperiode(
                        LocalDate.of(2020, 4, 10),
                        LocalDate.of(2020, 4, 12),
                        3, 1000.0
                    )
                ),
                GammeltRefusjonskravStatus.MOTTATT,
                "oppgave-id-234234",
                "joark-ref-1232"
            )
        )
        assertThat(repo.getByIkkeIndeksertInflux(100)).hasSize(1)
        repo.delete(krav2.id)
    }

    @Test
    fun `Ignorerer ikke opprettetAv ved serialisering til database`() {
        val savedKrav = repo.insert(refusjonskrav)

        repo.getById(savedKrav.id)

        assertThat(savedKrav).isNotNull()
        assertThat(savedKrav.opprettetAv).isEqualTo(TestData.opprettetAv)
    }
}
