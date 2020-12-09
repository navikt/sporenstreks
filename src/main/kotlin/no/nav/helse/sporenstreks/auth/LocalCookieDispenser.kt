package no.nav.helse.sporenstreks.auth

import io.ktor.application.*
import io.ktor.config.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import io.prometheus.client.hotspot.DefaultExports
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.net.InetAddress

@KtorExperimentalAPI
fun Application.localCookieDispenser(config: ApplicationConfig) {

    val server = MockOAuth2Server()
    server.start(port = 6666)

    DefaultExports.initialize()

    routing {
        get("/local/cookie-please") {
                val token = server.issueToken(call.request.queryParameters["subject"].toString())
                val cookieName = config.configList("no.nav.security.jwt.issuers")[0].property("cookie_name").getString()
                call.response.cookies.append(Cookie(cookieName, token.serialize(), CookieEncoding.RAW, domain = "localhost", path = "/"))

            if (call.request.queryParameters["redirect"] != null) {
                call.respondText("<script>window.location.href='" + call.request.queryParameters["redirect"] + "';</script>", ContentType.Text.Html, HttpStatusCode.OK)
            } else {
                call.respondText("Cookie Set", ContentType.Text.Plain, HttpStatusCode.OK)
            }
        }
    }
}