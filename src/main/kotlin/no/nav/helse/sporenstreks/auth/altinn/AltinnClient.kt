package no.nav.helse.sporenstreks.auth.altinn

import io.ktor.client.HttpClient
import io.ktor.client.features.ServerResponseException
import io.ktor.client.request.get
import io.ktor.client.statement.readText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import no.nav.helse.sporenstreks.auth.AuthorizationsRepository
import no.nav.helse.sporenstreks.domene.AltinnOrganisasjon
import no.nav.helse.sporenstreks.selfcheck.HealthCheck
import no.nav.helse.sporenstreks.selfcheck.HealthCheckType
import org.slf4j.LoggerFactory

class AltinnClient(
        altinnBaseUrl: String,
        private val apiGwApiKey: String,
        private val altinnApiKey: String,
        serviceCode: String,
        private val httpClient: HttpClient) : AuthorizationsRepository, HealthCheck {
    override val healthCheckType = HealthCheckType.READYNESS

    private val logger: org.slf4j.Logger = LoggerFactory.getLogger("AltinnClient")

    init {
        logger.debug("""Altinn Config:
                    altinnBaseUrl: $altinnBaseUrl
                    apiGwApiKey: ${apiGwApiKey.take(1)}.....
                    altinnApiKey: ${altinnApiKey.take(1)}.....
                    serviceCode: $serviceCode
        """.trimIndent())
    }

    private val baseUrl = "$altinnBaseUrl/reportees/?ForceEIAuthentication&serviceEdition=1&serviceCode=$serviceCode&subject="

    /**
     * @return en liste over organisasjoner og/eller personer den angitte personen har rettigheten for
     */
    override fun hentOrgMedRettigheterForPerson(identitetsnummer: String): Set<AltinnOrganisasjon> {
        logger.debug("Henter organisasjoner for ${identitetsnummer.take(5)}XXXXX")

        val url = baseUrl + identitetsnummer
        return runBlocking {
            try {
                httpClient.get<Set<AltinnOrganisasjon>>(url) {
                    headers.append("X-NAV-APIKEY", apiGwApiKey)
                    headers.append("APIKEY", altinnApiKey)
                }
            } catch(ex: ServerResponseException) {
                // midlertidig hook for å detektere at det tok for lang tid å hente rettigheter
                // brukeren/klienten kan prøve igjen når dette skjer siden altinn svarer raskere gang nummer 2
                if (ex.response.status == HttpStatusCode.BadGateway) throw AltinnBrukteForLangTidException()
                else throw ex
            }
        }
    }

    override suspend fun doHealthCheck() {
        try {
            // TODO: Få på plass en ok helsesjekk her. Litt vanskelig siden det ikke er noe naturlig å kalle,
            // og kubernetes hamrer denne helsesjekken ved oppstart om den feiler
            // hentOrgMedRettigheterForPerson("01065500791")
        } catch (ex: io.ktor.client.features.ClientRequestException) {
            if (!(ex.response.status == HttpStatusCode.BadRequest && ex.response.readText().contains("Invalid social security number"))) {
                throw ex
            }
        }
    }
}

class AltinnBrukteForLangTidException : Exception(
        "Altinn brukte for lang tid til å svare på forespørsleen om tilganger. Prøv igjen om litt."
)