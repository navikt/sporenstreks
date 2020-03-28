package no.nav.helse.sporenstreks.integrasjon.rest.aktor

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.url
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.helse.sporenstreks.integrasjon.rest.sts.STSClient

class AktorConsumer(val stsClient: STSClient,
                    val username: String,
                    val url: String,
                    val httpClient: HttpClient) {

    fun getAktorId(fnr: String): String {
        return getIdent(fnr, "AktoerId")
    }

    private fun getIdent(sokeIdent: String, identgruppe: String): String {
        val response = runBlocking {
            httpClient.get<AktorResponse> {
                url("$url/identer?gjeldende=true&identgruppe=$identgruppe")
                headers.append("Authorization", "Bearer " + stsClient.getOidcToken())
                headers.append("Nav-Consumer-Id", username)
                headers.append("Nav-Call-Id", "TODO")
                headers.append("Nav-Personidenter", sokeIdent)
                contentType(io.ktor.http.ContentType.Application.FormUrlEncoded)
            }
        }[sokeIdent]
        if (response?.feilmelding?.isNotEmpty()!!) {
            throw Exception()
        }
        return response?.identer?.first { it.gjeldende!! }?.ident
                ?: throw Exception()
    }
}