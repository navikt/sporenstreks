package no.nav.helse.sporenstreks.integrasjon.rest.dokarkiv

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.helse.sporenstreks.domene.Refusjonskrav
import no.nav.helse.sporenstreks.integrasjon.rest.sts.STSClient
import org.slf4j.LoggerFactory

interface DokarkivKlient {
    fun journalførDokument(dokument: String, refusjonskrav: Refusjonskrav, callId: String): String
}

class MockDokarkivKlient : DokarkivKlient {
    override fun journalførDokument(dokument: String, refusjonskrav: Refusjonskrav, callId: String): String {
        return "id"
    }
}

//TODO Legg til helsesjekk?

class DokarkivKlientImpl(
        private val dokarkivBaseUrl: String,
        private val httpClient: HttpClient,
        private val stsClient: STSClient) : DokarkivKlient {

    private val logger: org.slf4j.Logger = LoggerFactory.getLogger("DokarkivClient")


    override fun journalførDokument(dokument: String, refusjonskrav: Refusjonskrav, callId: String): String {
        logger.debug("Journalfører dokument");
        val url = "$dokarkivBaseUrl/rest/journalpostapi/v1/journalpost?forsoekFerdigstill=true"
        val response = runBlocking {
            httpClient.post<JournalpostResponse> {
                url(url)
                headers.append("Authorization", "Bearer " + stsClient.getOidcToken())
                headers.append("Nav-Call-Id", callId)
                contentType(io.ktor.http.ContentType.Application.Json)
                body = JournalpostRequest(
                        bruker = Bruker(
                                id = refusjonskrav.identitetsnummer
                        ),
                        avsenderMottaker = AvsenderMottaker(
                                id = refusjonskrav.virksomhetsnummer,
                                navn = "Arbeidsgiver"
                        ),
                        dokumenter = listOf(Dokument(
                                dokumentVarianter = listOf(DokumentVariant(
                                        fysiskDokument = dokument
                                ))
                        )),
                        datoMottatt = refusjonskrav.opprettet.toLocalDate()
                )
            }
        }
        assert(response.journalpostFerdigstilt)
        return response.journalpostId
    }
}

