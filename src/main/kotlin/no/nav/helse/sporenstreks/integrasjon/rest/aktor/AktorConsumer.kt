package no.nav.helse.sporenstreks.integrasjon.rest.aktor

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.url
import kotlinx.coroutines.runBlocking
import no.nav.helse.sporenstreks.integrasjon.rest.sts.STSClient

interface AktorConsumer {
    fun getAktorId(fnr: String): String
}

class MockAktorConsumer : AktorConsumer {
    override fun getAktorId(fnr: String): String {
        return "aktorId"
    }
}

class AktorConsumerImpl(val stsClient: STSClient,
                        val username: String,
                        val baseUrl: String,
                        val httpClient: HttpClient) : AktorConsumer {

    override fun getAktorId(fnr: String): String {
        return getIdent(fnr, "AktoerId")
    }

    private fun getIdent(sokeIdent: String, identgruppe: String): String {
        val response = runBlocking {
            httpClient.get<AktorResponse> {
                url("$baseUrl/identer?gjeldende=true&identgruppe=$identgruppe")
                headers.append("Authorization", "Bearer " + stsClient.getOidcToken())
                headers.append("Nav-Consumer-Id", username)
                headers.append("Nav-Personidenter", sokeIdent)
            }
        }[sokeIdent]
        response?.feilmelding?.isNotEmpty()?.let {
            throw Exception("Feil ved henting av aktør: $it")
        }
        return response?.identer?.first { it.gjeldende!! }?.ident
                ?: throw Exception("Finner ikke aktørId")
    }
}