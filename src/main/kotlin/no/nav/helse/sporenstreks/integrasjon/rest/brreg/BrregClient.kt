package no.nav.helse.sporenstreks.integrasjon.rest.brreg

import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*

interface BrregClient {
    suspend fun getVirksomhetsNavn(orgnr: String): String
}

class MockBrregClient : BrregClient {
    override suspend fun getVirksomhetsNavn(orgnr: String): String {
        return "Stark Industries"
    }
}

class BrregClientImp(private val httpClient: HttpClient, private val brregUrl: String) :
    BrregClient {
    override suspend fun getVirksomhetsNavn(orgnr: String): String {
        return try {
            httpClient.get<UnderenheterNavnResponse>(brregUrl + orgnr).navn
        } catch (cause: ClientRequestException) {
            if (404 == cause.response.status.value) {
                "Ukjent arbeidsgiver"
            } else {
                throw cause
            }
        }
    }
}
