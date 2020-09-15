package no.nav.helse.sporenstreks.integrasjon.rest.sts

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.security.token.support.core.jwt.JwtToken
import org.apache.cxf.Bus
import org.apache.cxf.BusFactory
import org.apache.cxf.binding.soap.Soap12
import org.apache.cxf.binding.soap.SoapMessage
import org.apache.cxf.endpoint.Client
import org.apache.cxf.frontend.ClientProxy
import org.apache.cxf.ws.policy.PolicyBuilder
import org.apache.cxf.ws.policy.PolicyEngine
import org.apache.cxf.ws.policy.attachment.reference.RemoteReferenceResolver
import org.apache.cxf.ws.security.SecurityConstants
import org.apache.neethi.Policy
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
    private val errorMessage = "Feilet ved kall til STS"

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
        if (isExpired(currentToken, Date.from(Instant.now().plusSeconds(300)))) {
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
                    ?: throw IllegalStateException(errorMessage)


            return JwtToken(accessToken)
        } catch (e: InterruptedException) {
            throw IllegalStateException(errorMessage, e)
        } catch (e: IOException) {
            throw IllegalStateException(errorMessage, e)
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

val STS_CLIENT_AUTHENTICATION_POLICY = "classpath:sts-policies/untPolicy.xml"
val STS_SAML_POLICY = "classpath:sts-policies/requestSamlPolicy.xml"

fun wsStsClient(stsUrl: String, credentials: Pair<String, String>): org.apache.cxf.ws.security.trust.STSClient {
    val bus = BusFactory.getDefaultBus()
    return org.apache.cxf.ws.security.trust.STSClient(bus).apply {
        isEnableAppliesTo = false
        isAllowRenewing = false
        location = stsUrl
        properties = mapOf(
                SecurityConstants.USERNAME to credentials.first,
                SecurityConstants.PASSWORD to credentials.second
        )
        setPolicy(bus.resolvePolicy(STS_CLIENT_AUTHENTICATION_POLICY))
    }
}

private fun Bus.resolvePolicy(policyUri: String): Policy {
    val registry = getExtension(PolicyEngine::class.java).registry
    val resolved = registry.lookup(policyUri)

    val policyBuilder = getExtension(PolicyBuilder::class.java)
    val referenceResolver = RemoteReferenceResolver("", policyBuilder)

    return resolved ?: referenceResolver.resolveReference(policyUri)
}

fun org.apache.cxf.ws.security.trust.STSClient.configureFor(servicePort: Any) {
    configureFor(servicePort, STS_SAML_POLICY)
}

fun org.apache.cxf.ws.security.trust.STSClient.configureFor(servicePort: Any, policyUri: String) {
    val client = ClientProxy.getClient(servicePort)
    client.configureSTS(this, policyUri)
}

fun Client.configureSTS(stsClient: org.apache.cxf.ws.security.trust.STSClient, policyUri: String = STS_SAML_POLICY) {
    requestContext[SecurityConstants.STS_CLIENT] = stsClient
    requestContext[SecurityConstants.CACHE_ISSUED_TOKEN_IN_ENDPOINT] = true
    setClientEndpointPolicy(bus.resolvePolicy(policyUri))
}

private fun Client.setClientEndpointPolicy(policy: Policy) {
    val policyEngine: PolicyEngine = bus.getExtension(PolicyEngine::class.java)
    val message = SoapMessage(Soap12.getInstance())
    val endpointPolicy = policyEngine.getClientEndpointPolicy(endpoint.endpointInfo, null, message)
    policyEngine.setClientEndpointPolicy(endpoint.endpointInfo, endpointPolicy.updatePolicy(policy, message))
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
