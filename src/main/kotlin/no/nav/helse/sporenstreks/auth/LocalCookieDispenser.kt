package no.nav.helse.sporenstreks.auth

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.config.ApplicationConfig
import io.ktor.http.ContentType
import io.ktor.http.Cookie
import io.ktor.http.CookieEncoding
import io.ktor.http.HttpStatusCode
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.util.KtorExperimentalAPI
import io.prometheus.client.hotspot.DefaultExports
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.OAuth2TokenProvider

@KtorExperimentalAPI
fun Application.localCookieDispenser(config: ApplicationConfig) {

    DefaultExports.initialize()
    var server = MockOAuth2Server()
    server.start()
    routing {
        get("/local/cookie-please") {
            if (config.property("koin.profile").getString() == "LOCAL") {
                val cookieName = config.configList("no.nav.security.jwt.issuers")[0].property("cookie_name").getString()
                val jwtToken = call.request.queryParameters["subject"]?.let { it1 -> server.issueToken(it1).serialize() }
                call.response.cookies.append(Cookie(cookieName, jwtToken.toString(), CookieEncoding.RAW, domain = "localhost", path = "/"))
            }

            if (call.request.queryParameters["redirect"] != null) {
                call.respondText("<script>window.location.href='" + call.request.queryParameters["redirect"] + "';</script>", ContentType.Text.Html, HttpStatusCode.OK)
            } else {
                call.respondText("Cookie Set", ContentType.Text.Plain, HttpStatusCode.OK)
            }
        }
    }
    server.shutdown()
}


class LocalOIDCWireMock() {
    companion object {
        var started = false

        fun start() {
            if (started) return

            fun stubOIDCProvider(server: WireMockServer) {
                WireMock.stubFor(WireMock.any(WireMock.urlPathEqualTo("/.well-known/openid-configuration")).willReturn(
                        WireMock.okJson("{\"jwks_uri\": \"${server.baseUrl()}/keys\", " +
                                "\"subject_types_supported\": [\"pairwise\"], " +
                                "\"issuer\": \"iss-localhost")))

                WireMock.stubFor(WireMock.any(WireMock.urlPathEqualTo("/keys")).willReturn(
                        WireMock.okJson(OAuth2TokenProvider().publicJwkSet().toString())))
            }

            val server = WireMockServer(WireMockConfiguration.options().port(6666))

            server.start()
            WireMock.configureFor(server.port())
            stubOIDCProvider(server)
            started = true
        }

    }
}

