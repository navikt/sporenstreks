package no.nav.helse.slowtests

import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.TestData
import no.nav.helse.sporenstreks.db.PostgresKvitteringRepository
import no.nav.helse.sporenstreks.db.createNonVaultHikariConfig
import no.nav.helse.sporenstreks.domene.Arbeidsgiverperiode
import no.nav.helse.sporenstreks.domene.Refusjonskrav
import no.nav.helse.sporenstreks.domene.RefusjonskravStatus
import no.nav.helse.sporenstreks.kvittering.Kvittering
import no.nav.helse.sporenstreks.kvittering.KvitteringStatus
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
import java.time.LocalDateTime

internal class PostgresKvitteringRepositoryTest : KoinComponent {

    lateinit var repo: PostgresKvitteringRepository
    lateinit var kvittering: Kvittering

    @BeforeEach
    internal fun setUp() {
        startKoin {
            loadKoinModules(common)

        }
        repo = PostgresKvitteringRepository(HikariDataSource(createNonVaultHikariConfig()), get())
        kvittering = repo.insert(Kvittering(
                tidspunkt = LocalDateTime.now(),
                virksomhetsnummer = TestData.validOrgNr,
                refusjonsListe = listOf(Refusjonskrav(
                        TestData.notValidIdentitetsnummer,
                        TestData.validOrgNr,
                        setOf(
                                Arbeidsgiverperiode(
                                        LocalDate.of(2020, 4, 1),
                                        LocalDate.of(2020, 4, 6),
                                        3, 1000.0
                                ), Arbeidsgiverperiode(
                                LocalDate.of(2020, 4, 10),
                                LocalDate.of(2020, 4, 12),
                                3, 1000.0
                        )),
                        RefusjonskravStatus.MOTTATT,
                        "oppgave-id-234234",
                        "joark-ref-1232"
                ))))
    }

    @AfterEach
    internal fun tearDown() {
        repo.delete(kvittering.id)
        stopKoin()
    }


    @Test
    fun `Kan hente lagret kvittering`() {
        val rs = repo.getById(kvittering.id)

        assertThat(rs).isNotNull
        assertThat(rs).isEqualTo(kvittering)
    }


    @Test
    fun `Kan hente fra status`() {
        val kvitteringListe = repo.getByStatus(KvitteringStatus.OPPRETTET, 10)
        assertThat(kvitteringListe.size).isEqualTo(1)
        assertThat(kvitteringListe.first()).isEqualTo(kvittering)
    }

    @Test
    fun `Kan oppdatere krav`() {
        val kvitteringListe = repo.getByStatus(KvitteringStatus.OPPRETTET, 10)
        val kvittering = kvitteringListe.first()


        kvittering.status = KvitteringStatus.SENDT

        repo.update(kvittering)

        val fromDb = repo.getById(kvittering.id)

        assertThat(kvittering).isEqualTo(fromDb)
    }

    @Test
    fun `Kan slette et refusjonskrav`() {
        val deletedCount = repo.delete(kvittering.id)
        assertThat(deletedCount).isEqualTo(1)
    }
}