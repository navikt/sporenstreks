package no.nav.helse.sporenstreks.datapakke

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.utils.RecurringJob
import no.nav.helse.arbeidsgiver.utils.loadFromResources
import no.nav.helse.sporenstreks.db.IStatsRepo
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime

class DatapakkePublisherJob(
    private val statsRepo: IStatsRepo,
    private val httpClient: HttpClient,
    private val datapakkeApiUrl: String,
    private val datapakkeId: String,
    private val om: ObjectMapper,
    private val applyWeeklyOnly: Boolean = false
) :
    RecurringJob(
        CoroutineScope(Dispatchers.IO),
        Duration.ofHours(3).toMillis()
    ) {
    override fun doJob() {
        val now = LocalDateTime.now()
        if (applyWeeklyOnly && now.dayOfWeek != DayOfWeek.MONDAY && now.hour != 0) {
            return // Ikke kjør jobben med mindre det er natt til mandag
        }
        val timeseries = statsRepo.getAntallKravStatsUke()

        // TODO: importer datapakkeJson
        val datapakkeTemplate = {}.toString()
        //val datapakkeTemplate = "datapakke/datapakke-im-varsel.json".loadFromResources()
        val populatedDatapakke = datapakkeTemplate
            .replace("@ukeSerie", timeseries.map { it.weekNumber }.joinToString())
            .replace("@web", timeseries.map { it.antall_web }.joinToString())
            .replace("@excel", timeseries.map { it.antall_excel }.joinToString())

        logger.info("genererte datapakke med data: $populatedDatapakke")

        runBlocking {
            val response = httpClient.put<HttpResponse>("$datapakkeApiUrl/$datapakkeId") {
                contentType(ContentType.Application.Json)
                body = om.readTree(populatedDatapakke)
            }
            logger.info("Oppdaterte datapakke $datapakkeId med respons ${response.readText()}")
        }
    }
}
