package no.nav.helse.sporenstreks.auth

import io.ktor.application.*
import io.ktor.config.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import no.nav.helse.arbeidsgiver.system.AppEnv
import no.nav.helse.arbeidsgiver.system.getEnvironment
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.slf4j.LoggerFactory

@KtorExperimentalAPI
fun Application.localCookieDispenser(config: ApplicationConfig) {
    val logger = LoggerFactory.getLogger("LocalCookieDispenser")
    val cookieName = config.configList("no.nav.security.jwt.issuers")[0].property("cookie_name").getString()
    val issuerName = config.configList("no.nav.security.jwt.issuers")[0].property("issuer_name").getString()
    val audience = config.configList("no.nav.security.jwt.issuers")[0].property("accepted_audience").getString()
    val domain = if (config.getEnvironment() == AppEnv.PREPROD) "dev.nav.no" else "localhost"

    val server = MockOAuth2Server()
    server.start(port = 6666)
    logger.info("Startet OAuth mock på ${server.wellKnownUrl(issuerName)}")

    routing {
        get("/local/cookie-please") {

            val token = server.issueToken(
                subject = call.request.queryParameters["subject"].toString(),
                issuerId = issuerName,
                audience = audience
            )
            call.response.cookies.append(Cookie(cookieName, token.serialize(), CookieEncoding.RAW, domain = domain, path = "/"))

            if (call.request.queryParameters["redirect"] != null) {
                call.respondText("<script>window.location.href='" + call.request.queryParameters["redirect"] + "';</script>", ContentType.Text.Html, HttpStatusCode.OK)
            } else {
                call.respondText("Cookie Set", ContentType.Text.Plain, HttpStatusCode.OK)
            }
        }
    }
}
