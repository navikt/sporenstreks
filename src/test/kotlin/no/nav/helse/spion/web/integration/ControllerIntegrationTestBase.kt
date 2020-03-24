package no.nav.helse.sporenstreks.web.integration

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.ktor.application.Application
import io.ktor.config.ApplicationConfig
import io.ktor.config.MapApplicationConfig
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.testing.*
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.TestData
import no.nav.security.token.support.test.JwkGenerator
import no.nav.security.token.support.test.JwtTokenGenerator
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.koin.test.KoinTest

@KtorExperimentalAPI
open class ControllerIntegrationTestBase : KoinTest {

    protected val defaultSubject = TestData.validIdentitetsnummer
    private val testConfig: ApplicationConfig
    protected val idTokenCookieName = "selvbetjening-idtoken"

    init {
        testConfig = MapApplicationConfig()
        addIntegrationTestConfigValues(testConfig)
    }

    fun <R> configuredTestApplication(moduleFunction: Application.() -> Unit, test: TestApplicationEngine.() -> R): R {
        return withApplication(createTestEnvironment()) {
            addIntegrationTestConfigValues(application.environment.config as MapApplicationConfig)
            moduleFunction(application)
            test()
        }
    }

    fun TestApplicationEngine.doAuthenticatedRequest(
            method: HttpMethod,
            uri: String,
            authenticatedSubject: String = defaultSubject,
            setup: TestApplicationRequest.() -> Unit = {}
    ): TestApplicationCall = handleRequest {
        this.uri = uri
        this.method = method
        val token = JwtTokenGenerator.createSignedJWT(authenticatedSubject)
        addHeader(HttpHeaders.Cookie, "$idTokenCookieName=${token.serialize()}")
        setup()
    }


    @KtorExperimentalAPI
    private fun addIntegrationTestConfigValues(config: MapApplicationConfig, acceptedIssuer: String = JwtTokenGenerator.ISS, acceptedAudience: String = JwtTokenGenerator.AUD) {
        config.apply {
            put("koin.profile", "TEST")
            put("no.nav.security.jwt.issuers.size", "1")
            put("no.nav.security.jwt.issuers.0.issuer_name", acceptedIssuer)
            put("no.nav.security.jwt.issuers.0.discoveryurl", server.baseUrl() + "/.well-known/openid-configuration")
            put("no.nav.security.jwt.issuers.0.accepted_audience", acceptedAudience)
            put("no.nav.security.jwt.issuers.0.cookie_name", idTokenCookieName)
        }
    }

    companion object {
        val server: WireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort())

        @BeforeAll
        @JvmStatic
        fun before() {
            server.start()
            WireMock.configureFor(server.port())
            stubOIDCProvider()
        }

        @AfterAll
        @JvmStatic
        fun after() {
            server.stop()
        }

        fun stubOIDCProvider() {
            WireMock.stubFor(WireMock.any(WireMock.urlPathEqualTo("/.well-known/openid-configuration")).willReturn(
                    WireMock.okJson("{\"jwks_uri\": \"${server.baseUrl()}/keys\", " +
                            "\"subject_types_supported\": [\"pairwise\"], " +
                            "\"issuer\": \"${JwtTokenGenerator.ISS}\"}")))

            WireMock.stubFor(WireMock.any(WireMock.urlPathEqualTo("/keys")).willReturn(
                    WireMock.okJson(JwkGenerator.getJWKSet().toPublicJWKSet().toString())))
        }
    }
}