package no.nav.helse.sporenstreks.integrasjon.rest.sts

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant
import java.util.*
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.UriBuilder


class STSClient(private val username: String, private val password: String, private val stsEndpoint: String) {

    private val httpClient: HttpClient
    private val endpointURI: URI
    private val basicAuth: String

    private var currentToken: String

    init {
        basicAuth = basicAuth(username, password)
        endpointURI = UriBuilder.fromPath(stsEndpoint)
                .queryParam("grant_type", "client_credentials")
                .queryParam("scope", "openid")
                .build()
        httpClient = HttpClient.newHttpClient()
        currentToken = tokenFromSTS
    }


    fun getOidcToken(): String {
        if (isExpired(currentToken)) {
            log.info("OIDC Token for srvoppgave expired, getting a new one from the STS")
            currentToken = tokenFromSTS
        }
        return currentToken
    }

    private fun isExpired(oidcToken: String): Boolean {
        val mapper = ObjectMapper()
        try {
            val tokenBody = String(Base64.getDecoder().decode(StringUtils.substringBetween(oidcToken, ".")))
            val node = mapper.readTree(tokenBody)
            val now = Instant.now()
            val expiry = Instant.ofEpochSecond(node["exp"].longValue()).minusSeconds(300) //5 min padding
            log.debug("OIDC token still not expired, checking that {} is after {} - ({} {})", now, expiry, now.epochSecond, expiry.epochSecond)
            if (now.isAfter(expiry)) {
                log.info("OIDC token expired {} is after {}", now, expiry)
                return true
            }
        } catch (e: Exception) {
            throw IllegalStateException("Klarte ikke parse oidc token fra STS", e)
        }
        return false
    }

    private val tokenFromSTS: String
        private get() {
            log.info("sts endpoint uri: $endpointURI")
            val request = HttpRequest.newBuilder()
                    .uri(endpointURI)
                    .header(HttpHeaders.AUTHORIZATION, basicAuth)
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build()
            try {
                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
                check(response.statusCode() == HttpURLConnection.HTTP_OK) { String.format("Feil oppsto under henting av token fra STS - %s", response.body()) }
                return ObjectMapper().readValue(response.body(), STSOidcResponse::class.java).access_token
                        ?: throw IllegalStateException("Feilet ved kall til STS")
            } catch (e: InterruptedException) {
                throw IllegalStateException("Feilet ved kall til STS", e)
            } catch (e: IOException) {
                throw IllegalStateException("Feilet ved kall til STS", e)
            }
        }

    private fun basicAuth(username: String, password: String): String {
        log.info("basic auth username: $username")
        return "Basic " + Base64.getEncoder().encodeToString("$username:$password".toByteArray())
    }

    companion object {
        private val log = LoggerFactory.getLogger(STSClient::class.java)
    }
}


class STSOidcResponse {
    var access_token: String? = null
    var token_type: String? = null
    var expires_in: Int? = null

}