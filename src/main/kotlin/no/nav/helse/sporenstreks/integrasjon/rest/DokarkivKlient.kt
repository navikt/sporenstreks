package no.nav.helse.sporenstreks.integrasjon.rest

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.url
import kotlinx.coroutines.runBlocking
import no.nav.helse.sporenstreks.domene.Refusjonskrav
import org.slf4j.LoggerFactory
import java.time.LocalDate

class DokarkivKlient(
        private val dokarkivBaseUrl: String,
        private val httpClient: HttpClient) {

    private val logger: org.slf4j.Logger = LoggerFactory.getLogger("DokarkivClient")


    fun journalførDokument(dokument: String, refusjonskrav: Refusjonskrav): String {
        logger.debug("Journalfører dokument");
        val url = "$dokarkivBaseUrl/journalpost?forsoekFerdigstill=true"
        return runBlocking {
            httpClient.post<String> {
                url(url)
                headers.append("AUTHORIZATION", "OIDCTOKEN")
                body = JournalpostResponse(
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
                        ))
                )
            }
        }
    }
}

data class JournalpostResponse(
        val tema: String = "SYK",
        val bruker: Bruker,
        val avsenderMottaker: AvsenderMottaker,
        val tittel: String = "Søknad om Refusjon i forbindelse med Corona", //TODO
        val dokumenter: List<Dokument>,
        val sak: Sak = Sak(),
        val datoMottatt: LocalDate = LocalDate.now() //TODO

)

data class Dokument(
        val brevkode: String = "CORONAREFUSJONSØKNAD", //TODO
        val dokumentKategori: String = "SOK", //TODO
        val tittel: String = "Søknad refusjon corona", //TODO
        val dokumentVarianter: List<DokumentVariant>
)

data class DokumentVariant(
        val filnavn: String = "coronasøknad.pdf", //TODO
        val filtype: String = "PDFA",
        val fysiskDokument: String,
        val variantFormat: String = "ARKIV"
)

data class Bruker(
        val id: String,
        val idType: String = "fnr"
)

data class AvsenderMottaker(
        val id: String,
        val idType: String = "ORGNR",
        val land: String = "NORGE", //TODO
        val navn: String //TODO
)

data class Sak(
        val sakstype: String = "GENERELL"
)