package no.nav.helse.spion.auth

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
import no.nav.security.token.support.test.JwkGenerator
import no.nav.security.token.support.test.JwtTokenGenerator

@KtorExperimentalAPI
fun Application.localCookieDispenser(config: ApplicationConfig) {

    DefaultExports.initialize()

    routing {
        get("/local/cookie-please") {
            if (config.property("koin.profile").getString() == "LOCAL") {
                val cookieName = config.configList("no.nav.security.jwt.issuers")[0].property("cookie_name").getString()
                call.response.cookies.append(Cookie(cookieName, JwtTokenGenerator.createSignedJWT(call.request.queryParameters["subject"]).serialize(), CookieEncoding.RAW, domain = "localhost", path = "/"))
            }

            if (call.request.queryParameters["redirect"] != null) {
                call.respondText("<script>window.location.href='" + call.request.queryParameters["redirect"] + "';</script>", ContentType.Text.Html, HttpStatusCode.OK)
            } else {
                call.respondText("Cookie Set", ContentType.Text.Plain, HttpStatusCode.OK)
            }
        }
    }
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
                                "\"issuer\": \"${JwtTokenGenerator.ISS}\"}")))

                WireMock.stubFor(WireMock.any(WireMock.urlPathEqualTo("/keys")).willReturn(
                        WireMock.okJson(JwkGenerator.getJWKSet().toPublicJWKSet().toString())))
            }

            val server = WireMockServer(WireMockConfiguration.options().port(6666))

            server.start()
            WireMock.configureFor(server.port())
            stubOIDCProvider(server)
            started = true
        }


    }
}

