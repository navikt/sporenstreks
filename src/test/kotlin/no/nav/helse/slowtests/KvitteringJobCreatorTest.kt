package no.nav.helse.slowtests

import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScope
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbStatus
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.PostgresBakgrunnsjobbRepository
import no.nav.helse.sporenstreks.db.KvitteringRepository
import no.nav.helse.sporenstreks.db.PostgresKvitteringRepository
import no.nav.helse.sporenstreks.db.createNonVaultHikariConfig
import no.nav.helse.sporenstreks.kvittering.Kvittering
import no.nav.helse.sporenstreks.kvittering.KvitteringStatus
import no.nav.helse.sporenstreks.prosessering.kvittering.KvitteringJobCreator
import no.nav.helse.sporenstreks.web.common
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.KoinComponent
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.get
import java.time.LocalDateTime
import java.util.*

@ExperimentalCoroutinesApi
internal class KvitteringJobCreatorTest : KoinComponent {

    lateinit var kvitteringJobCreator: KvitteringJobCreator
    lateinit var kvitteringRepo: KvitteringRepository
    lateinit var bakgrunnsjobbRepo: BakgrunnsjobbRepository

    val id = UUID.randomUUID()

    private val testCoroutineScope = TestCoroutineScope()

    @BeforeEach
    internal fun setUp() {
        startKoin {
            loadKoinModules(common)
        }
        val ds = HikariDataSource(createNonVaultHikariConfig())
        kvitteringRepo = PostgresKvitteringRepository(ds, get())
        bakgrunnsjobbRepo = PostgresBakgrunnsjobbRepository(ds)
        kvitteringJobCreator = KvitteringJobCreator(ds, kvitteringRepo, bakgrunnsjobbRepo, get(), testCoroutineScope, 1)

        kvitteringJobCreator.startAsync(retryOnFail = true)

        kvitteringRepo.insert(Kvittering(id, "1", emptyList(), LocalDateTime.now()))
    }

    @AfterEach
    internal fun tearDown() {
        kvitteringRepo.delete(id)
    }

    @Test
    fun `plukker opp opprettet kvittering og oppretter en bakgrunnsjobb`() {
        assertThat(kvitteringRepo.getByStatus(KvitteringStatus.OPPRETTET, 250)).hasSize(1)
        testCoroutineScope.advanceTimeBy(1)
        assertThat(kvitteringRepo.getByStatus(KvitteringStatus.OPPRETTET, 250)).hasSize(0)
        assertThat(kvitteringRepo.getByStatus(KvitteringStatus.JOBB, 250)).hasSize(1)

        val jobber = bakgrunnsjobbRepo.findByKjoeretidBeforeAndStatusIn(LocalDateTime.now().plusMinutes(1), setOf(BakgrunnsjobbStatus.OPPRETTET))
        assertThat(jobber).hasSize(1)
        bakgrunnsjobbRepo.delete(jobber.first().uuid)

    }


}
