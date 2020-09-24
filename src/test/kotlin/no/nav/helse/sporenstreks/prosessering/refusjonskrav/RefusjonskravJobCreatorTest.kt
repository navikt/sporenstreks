package no.nav.helse.sporenstreks.prosessering.refusjonskrav

import com.fasterxml.jackson.databind.ObjectMapper
import com.zaxxer.hikari.HikariDataSource
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScope
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbStatus
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.MockBakgrunnsjobbRepository
import no.nav.helse.sporenstreks.db.MockRefusjonskravRepo
import no.nav.helse.sporenstreks.db.RefusjonskravRepository
import no.nav.helse.sporenstreks.domene.Refusjonskrav
import no.nav.helse.sporenstreks.domene.RefusjonskravStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import javax.sql.DataSource

@ExperimentalCoroutinesApi
internal class RefusjonskravJobCreatorTest {

    val ds: DataSource = mockk<HikariDataSource>(relaxed = true)
    val bakgrunnRepo: BakgrunnsjobbRepository = MockBakgrunnsjobbRepository()
    val kravRepo: RefusjonskravRepository = MockRefusjonskravRepo()
    val testCoroutineScope = TestCoroutineScope()

    val jobCreator = RefusjonskravJobCreator(ds, kravRepo, bakgrunnRepo, ObjectMapper(), testCoroutineScope, 1)


    @BeforeEach
    internal fun setup() {
        jobCreator.startAsync(true)
    }

    @Test
    fun `Oppretter jobb for et mottatt krav`() {
        kravRepo.insert(Refusjonskrav("1", "1", emptySet()))
        testCoroutineScope.advanceTimeBy(1)
        assertThat(kravRepo.getByStatus(RefusjonskravStatus.MOTTATT, 250)).isEmpty()
        assertThat(kravRepo.getByStatus(RefusjonskravStatus.JOBB, 250)).hasSize(1)
        assertThat(bakgrunnRepo.findByKjoeretidBeforeAndStatusIn(LocalDateTime.now(), setOf(BakgrunnsjobbStatus.OPPRETTET))).hasSize(1)
    }


    @Test
    fun `Oppretter jobb for et feilet krav`() {
        kravRepo.insert(Refusjonskrav("1", "1", emptySet(), status = RefusjonskravStatus.FEILET))
        testCoroutineScope.advanceTimeBy(1)
        assertThat(kravRepo.getByStatus(RefusjonskravStatus.FEILET, 250)).isEmpty()
        assertThat(kravRepo.getByStatus(RefusjonskravStatus.JOBB, 250)).hasSize(1)
        assertThat(bakgrunnRepo.findByKjoeretidBeforeAndStatusIn(LocalDateTime.now(), setOf(BakgrunnsjobbStatus.OPPRETTET))).hasSize(1)
    }

}
