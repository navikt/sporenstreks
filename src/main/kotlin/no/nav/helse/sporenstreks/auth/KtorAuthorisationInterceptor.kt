package no.nav.helse.sporenstreks.auth

import io.ktor.config.ApplicationConfig
import io.ktor.request.ApplicationRequest
import io.ktor.util.KtorExperimentalAPI
import no.nav.security.token.support.core.jwt.JwtToken
import java.time.Instant
import java.util.*

@KtorExperimentalAPI
fun hentIdentitetsnummerFraLoginToken(config: ApplicationConfig, request: ApplicationRequest): String {
    val cookieName = config.configList("no.nav.security.jwt.issuers")[0].property("cookie_name").getString()

    val tokenString = request.cookies[cookieName]
        ?: request.headers["Authorization"]?.replaceFirst("Bearer ", "")
        ?: throw IllegalAccessException("Du må angi et identitetstoken som cookieen $cookieName eller i Authorization-headeren")

    val pid = JwtToken(tokenString).jwtTokenClaims.get("pid")
    return pid?.toString() ?: JwtToken(tokenString).subject
}

@KtorExperimentalAPI
fun hentUtløpsdatoFraLoginToken(config: ApplicationConfig, request: ApplicationRequest): Date {
    val cookieName = config.configList("no.nav.security.jwt.issuers")[0].property("cookie_name").getString()

    val tokenString = request.cookies[cookieName]
        ?: request.headers["Authorization"]?.replaceFirst("Bearer ", "")
        ?: throw IllegalAccessException("Du må angi et identitetstoken som cookie'en $cookieName eller i Authorization-headeren")

    return JwtToken(tokenString).jwtTokenClaims.expirationTime ?: Date.from(Instant.MIN)
}
