package no.nav.helse.sporenstreks.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.zaxxer.hikari.HikariDataSource
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbService
import no.nav.helse.sporenstreks.db.KvitteringRepository
import no.nav.helse.sporenstreks.db.MockKvitteringRepository
import no.nav.helse.sporenstreks.db.MockRefusjonskravRepo
import no.nav.helse.sporenstreks.db.RefusjonskravRepository
import no.nav.helse.sporenstreks.domene.Refusjonskrav
import no.nav.helse.sporenstreks.prosessering.kvittering.KvitteringProcessor
import no.nav.helse.sporenstreks.prosessering.refusjonskrav.RefusjonskravProcessor
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import javax.sql.DataSource

internal class PostgresRefusjonskravServiceTest {


    val ds: DataSource = mockk<HikariDataSource>(relaxed = true)
    private val bakgrunnsjobbRepositoryMock = mockk<BakgrunnsjobbRepository>(relaxed = true)

    val bakgrunnsServiceSpy = spyk(BakgrunnsjobbService(bakgrunnsjobbRepositoryMock))
    val kravRepo: RefusjonskravRepository = spyk(MockRefusjonskravRepo())
    val kvitteringRepo: KvitteringRepository = spyk(MockKvitteringRepository())

    val service = PostgresRefusjonskravService(ds, kravRepo, kvitteringRepo, bakgrunnsServiceSpy, ObjectMapper())

    @BeforeEach
    internal fun setUp() {
        bakgrunnsServiceSpy.registrer(KvitteringProcessor(mockk(), mockk(), mockk()))
        bakgrunnsServiceSpy.registrer(RefusjonskravProcessor(mockk(), mockk(), mockk(), mockk(), mockk()))
    }

    @Test
    fun `ett krav lagres også med kvittering og to jobber`() {
        service.saveKravWithKvittering(Refusjonskrav("1", "1", "1", emptySet()))
        verify(exactly = 2) { bakgrunnsjobbRepositoryMock.save(any(), any())}
        verify(exactly = 1) { kvitteringRepo.insert(any(), any()) }
        verify(exactly = 1) { kravRepo.insert(any(), any()) }
    }

    @Test
    fun `to krav lagres også med en kvittering og to jobber hver`() {
        service.saveKravListWithKvittering(mapOf(Pair(0, Refusjonskrav("1", "1", "1", emptySet())), (Pair(1, Refusjonskrav("1", "1", "1", emptySet())))))
        verify(exactly = 1) { kvitteringRepo.insert(any(), any()) }
        verify(exactly = 2) { kravRepo.insert(any(), any()) }
        verify(exactly = 3) { bakgrunnsjobbRepositoryMock.save(any(), any())}
    }

    @Test
    fun `to krav lagres også med en kvittering og to jobber hver bulk`() {
        service.bulkInsert(listOf(Refusjonskrav("1", "1", "1", emptySet()), Refusjonskrav("1", "1", "1", emptySet())))
        verify(exactly = 3) { bakgrunnsjobbRepositoryMock.save(any(), any())}
        verify(exactly = 1) { kvitteringRepo.insert(any(), any()) }
        verify(exactly = 1) { kravRepo.bulkInsert(any(), any()) }
    }

}
