package no.nav.helse.sporenstreks.web.dto.validation

import org.valiktor.ConstraintViolation
import org.valiktor.i18n.toMessage
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
        URI.create("urn:sporenstreks:validation-error"),
        "Valideringen av input feilet",
        422,
        "Ett eller flere felter har feil."
)

class ValidationProblemDetail(
        val validationType: String, val message: String, val propertyPath: String, val invalidValue: Any?)

fun ConstraintViolation.getContextualMessage(): String {
    return when {
        (this.constraint.name =="GreaterOrEqual" && this.property.endsWith("beloep")) ->  "Refusjonsbeløpet må være et posthisivt tall"
        (this.constraint.name =="GreaterOrEqual" && this.property.endsWith(".tom")) ->  "Fra-dato må være før til-dato"
        (this.constraint.name =="LessOrEqual" && this.property.endsWith(".tom")) ->  "Det kan ikke kreves refusjon for datoer fremover i tid"
        else -> this.toMessage().message
    }
}

/**
 * Problem extension for excel-validation-feil.
 * Inneholder en liste over rader og kolonner som feilet parsing eller
 */
class ExcelProblem(
        val problemDetails: Set<ExcelProblemDetail>
) : Problem(
        URI.create("urn:sporenstreks:excel-error"),
        "Det var en eller flere feil med excelarket",
        422,
        "Ett eller flere rader/kolonner har feil."
)

class ExcelProblemDetail(
        val message: String, val row: String, val column: String)

