package no.nav.helse.slowtests

import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.TestData
import no.nav.helse.sporenstreks.db.PostgresRefusjonskravRepository
import no.nav.helse.sporenstreks.db.createLocalHikariConfig
import no.nav.helse.sporenstreks.domene.Arbeidsgiverperiode
import no.nav.helse.sporenstreks.domene.Refusjonskrav
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
import java.time.LocalDate

internal class PostgresRefusjonskravRepositoryTest : KoinComponent {

    lateinit var repo: PostgresRefusjonskravRepository

    val refusjonskrav = Refusjonskrav(
            TestData.validIdentitetsnummer,
            TestData.notValidIdentitetsnummer,
            TestData.validOrgNr,
            setOf(
                    Arbeidsgiverperiode(
                            LocalDate.of(2020, 4, 1),
                            LocalDate.of(2020, 4, 6),
                            3
                    ), Arbeidsgiverperiode(
                    LocalDate.of(2020, 4, 10),
                    LocalDate.of(2020, 4, 12),
                    3
            )),
            6612.23,
            "joark-ref-1232",
            "oppgave-id-234234"
    )

    @BeforeEach
    internal fun setUp() {
        startKoin {
            loadKoinModules(common)

        }
        repo = PostgresRefusjonskravRepository(HikariDataSource(createLocalHikariConfig()), get())
        repo.insert(refusjonskrav)
    }

    @AfterEach
    internal fun tearDown() {
        repo.delete(refusjonskrav.id)
        stopKoin()

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
    fun `Sletter et refusjonskrav`() {
        val deletedCount = repo.delete(refusjonskrav.id)
        assertThat(deletedCount).isEqualTo(1)
    }
}