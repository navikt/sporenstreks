package no.nav.helse.sporenstreks.integrasjon.rest.sts

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.security.token.support.core.jwt.JwtToken
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


class STSClient(username: String, password: String, stsEndpoint: String) {

    private val httpClient: HttpClient
    private val endpointURI: URI
    private val basicAuth: String

    private var currentToken: JwtToken

    init {
        basicAuth = basicAuth(username, password)
        endpointURI = UriBuilder.fromPath(stsEndpoint)
                .queryParam("grant_type", "client_credentials")
                .queryParam("scope", "openid")
                .build()
        httpClient = HttpClient.newHttpClient()
        currentToken = requestToken()
    }


    fun getOidcToken(): String {
        if (isExpired(currentToken, Date.from(Instant.now().minusSeconds(600)))) {
            log.info("OIDC Token is expired, getting a new one from the STS")
            currentToken = requestToken()
            log.info("Hentet nytt token fra sts som går ut ${currentToken.jwtTokenClaims.expirationTime}")
        }
        return currentToken.tokenAsString
    }

    private fun requestToken(): JwtToken {
        log.info("sts endpoint uri: $endpointURI")
        val request = HttpRequest.newBuilder()
                .uri(endpointURI)
                .header(HttpHeaders.AUTHORIZATION, basicAuth)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build()
        try {
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            check(response.statusCode() == HttpURLConnection.HTTP_OK) { String.format("Feil oppsto under henting av token fra STS - %s", response.body()) }

            val accessToken = ObjectMapper().readValue(response.body(), STSOidcResponse::class.java).access_token
                    ?: throw IllegalStateException("Feilet ved kall til STS")


            return JwtToken(accessToken)
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

fun isExpired(jwtToken: JwtToken, date: Date): Boolean {
    return date.after(jwtToken.jwtTokenClaims.expirationTime) &&
            jwtToken.jwtTokenClaims.expirationTime.before(date)
}


class STSOidcResponse {
    var access_token: String? = null
    var token_type: String? = null
    var expires_in: Int? = null
}