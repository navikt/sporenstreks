package no.nav.helse.spion.auth

import io.ktor.config.ApplicationConfig
import io.ktor.request.ApplicationRequest
import io.ktor.util.KtorExperimentalAPI
import no.nav.security.token.support.core.jwt.JwtToken

@KtorExperimentalAPI
fun hentIdentitetsnummerFraLoginToken(config: ApplicationConfig, request: ApplicationRequest): String {
    val cookieName = config.configList("no.nav.security.jwt.issuers")[0].property("cookie_name").getString()

    val tokenString = request.cookies[cookieName]
            ?: request.headers["Authorization"]?.replaceFirst("Bearer ", "")
            ?: throw IllegalAccessException("Du må angi et identitetstoken som cookieen $cookieName eller i Authorization-headeren")

    return JwtToken(tokenString).subject
}
