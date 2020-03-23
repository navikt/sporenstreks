package no.nav.helse.spion.web.dto.validation

import java.net.URI


/**
 * Tilbakemeldings-standard basert på
 *
 * https://tools.ietf.org/html/rfc7807#page-5
 *
 * Hvis du trenger å gi klienter tilbakemelding som inneholder
 * mer informasjon kan du arve fra denne klassen. ValidationProblem
 * er et eksempel på dette som inneholder valideringsfeil.
 */
open class Problem(
        val type: URI = URI.create("about:blank"),
        val title: String,
        val status: Int? = 500,
        val detail: String? = null,
        val instance: URI = URI.create("about:blank")
)

/**
 * Problem extension for input-validation-feil.
 * Inneholder en liste over properties som feilet validering
 */
class ValidationProblem(
        val violations: Set<ValidationProblemDetail>
) : Problem(
        URI.create("urn:spion:validation-error"),
        "Valideringen av input feilet",
        422,
        "Ett eller flere felter har feil."
)

class ValidationProblemDetail(
        val validationType: String, val message: String, val propertyPath: String, val invalidValue: Any?)
