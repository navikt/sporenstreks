package no.nav.helse.sporenstreks.integrasjon.rest.dokarkiv

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.helse.sporenstreks.domene.Refusjonskrav
import org.slf4j.LoggerFactory

interface DokarkivKlient {
    fun journalførDokument(dokument: String, refusjonskrav: Refusjonskrav): String
}

class MockDokarkivKlient : DokarkivKlient {
    override fun journalførDokument(dokument: String, refusjonskrav: Refusjonskrav): String {
        return "id"
    }
}

class DokarkivKlientImpl(
        private val dokarkivBaseUrl: String,
        private val httpClient: HttpClient) : DokarkivKlient {

    private val logger: org.slf4j.Logger = LoggerFactory.getLogger("DokarkivClient")


    override fun journalførDokument(dokument: String, refusjonskrav: Refusjonskrav): String {
        logger.debug("Journalfører dokument");
        val url = "$dokarkivBaseUrl/journalpost?forsoekFerdigstill=true"
        return runBlocking {
            httpClient.post<JournalpostResponse> {
                url(url)
                headers.append("AUTHORIZATION", "OIDCTOKEN")
                contentType(io.ktor.http.ContentType.Application.Json)
                body = JournalpostRequest(
                        bruker = Bruker(
                                id = refusjonskrav.identitetsnummer
                        ),
                        avsenderMottaker = AvsenderMottaker(
                                id = refusjonskrav.virksomhetsnummer,
                                navn = "TODO"
                        ),
                        dokumenter = listOf(Dokument(
                                dokumentVarianter = listOf(DokumentVariant(
                                        fysiskDokument = dokument
                                ))
                        )),
                        datoMottatt = refusjonskrav.opprettet.toLocalDate()
                )
            }.journalpostId
        }
    }
}

