package no.nav.helse.sporenstreks.web

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.config.ApplicationConfig
import io.ktor.features.ContentNegotiation
import io.ktor.features.DataConversion
import io.ktor.features.ParameterConversionException
import io.ktor.features.StatusPages
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.JacksonConverter
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Locations
import io.ktor.response.respond
import io.ktor.routing.routing
import io.ktor.util.DataConversionException
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.sporenstreks.auth.localCookieDispenser
import no.nav.helse.sporenstreks.nais.nais
import no.nav.helse.sporenstreks.web.api.sporenstreks
import no.nav.helse.sporenstreks.web.dto.validation.Problem
import no.nav.helse.sporenstreks.web.dto.validation.ValidationProblem
import no.nav.helse.sporenstreks.web.dto.validation.ValidationProblemDetail
import no.nav.security.token.support.ktor.tokenValidationSupport
import org.koin.ktor.ext.Koin
import org.koin.ktor.ext.get
import org.slf4j.LoggerFactory
import org.valiktor.ConstraintViolationException
import org.valiktor.i18n.toMessage
import java.lang.reflect.InvocationTargetException
import java.net.URI
import java.time.LocalDate
import java.util.*
import javax.ws.rs.ForbiddenException


@KtorExperimentalLocationsAPI
@KtorExperimentalAPI
fun Application.sporenstreksModule(config: ApplicationConfig = environment.config) {
    install(Koin) {
        modules(selectModuleBasedOnProfile(config))
    }

    install(Authentication) {
        tokenValidationSupport(config = config)
    }

    install(Locations)

    install(ContentNegotiation) {
        val commonObjectMapper = get<ObjectMapper>()
        register(ContentType.Application.Json, JacksonConverter(commonObjectMapper))
    }

    install(DataConversion) {
        convert<LocalDate> {
            decode { values, _ ->
                values.singleOrNull()?.let { LocalDate.parse(it) }
            }

            encode { value ->
                when (value) {
                    null -> listOf()
                    is LocalDate -> listOf(value.toString())
                    else -> throw DataConversionException("Cannot convert $value as LocalDate")
                }
            }
        }
    }

    install(StatusPages) {
        val LOGGER = LoggerFactory.getLogger("StatusPages")

        suspend fun handleUnexpectedException(call: ApplicationCall, cause: Throwable) {
            val errorId = UUID.randomUUID()
            val userAgent = call.request.headers.get("User-Agent") ?: "Ukjent"
            LOGGER.error("Uventet feil, $errorId med useragent $userAgent", cause)
            val problem = Problem(
                    type = URI.create("urn:sporenstreks:uventet-feil"),
                    title = "Uventet feil",
                    detail = cause.message,
                    instance = URI.create("urn:sporenstreks:uventent-feil:$errorId")
            )
            call.respond(HttpStatusCode.InternalServerError, problem)
        }

        suspend fun handleValidationError(call: ApplicationCall, cause: ConstraintViolationException) {
            val problems = cause.constraintViolations.map {
                when {
                    (it.constraint.name == "GreaterOrEqual" && it.property.endsWith("beloep")) -> ValidationProblemDetail(it.constraint.name, "Refusjonsbeløpet må være et positivt tall", it.property, it.value)
                    (it.constraint.name == "GreaterOrEqual" && it.property.endsWith(".tom")) -> ValidationProblemDetail(it.constraint.name, "Fra-dato må være før til-dato", it.property, it.value)
                    (it.constraint.name == "LessOrEqual" && it.property.endsWith(".tom")) -> ValidationProblemDetail(it.constraint.name, "Det kan ikke kreves refusjon for datoer fremover i tid", it.property, it.value)
                    else -> {
                        ValidationProblemDetail(it.constraint.name, it.toMessage().message, it.property, it.value)
                    }
                }
            }.toSet()

            call.respond(HttpStatusCode.UnprocessableEntity, ValidationProblem(problems))
        }

        exception<InvocationTargetException> { cause ->
            when (cause.targetException) {
                is ConstraintViolationException -> handleValidationError(call, cause.targetException as ConstraintViolationException)
                else -> handleUnexpectedException(call, cause)
            }
        }

        exception<ForbiddenException> {
            call.respond(
                    HttpStatusCode.Forbidden,
                    Problem(URI.create("urn:sporenstreks:forbidden"), "Ingen tilgang", HttpStatusCode.Forbidden.value)
            )
        }

        exception<Throwable> { cause ->
            handleUnexpectedException(call, cause)
        }

        exception<ParameterConversionException> { cause ->
            call.respond(
                    HttpStatusCode.BadRequest,
                    ValidationProblem(setOf(
                            ValidationProblemDetail("ParameterConversion", "Paramteret kunne konverteres til ${cause.type}", cause.parameterName, null))
                    )
            )
        }

        exception<MissingKotlinParameterException> { cause ->
            call.respond(
                    HttpStatusCode.BadRequest,
                    ValidationProblem(setOf(
                            ValidationProblemDetail("NotNull", "Det angitte feltet er påkrevd", cause.path.joinToString(".") { it.fieldName }, "null"))
                    )
            )
        }

        exception<JsonMappingException> { cause ->
            // Siden valideringen foregår i init {} blokken vil
            // Jackson kunne støte på constrainViolations under de-serialisering.
            // disse vil vi vise til klienten som valideringsfeil

            when (cause.cause) {
                is ConstraintViolationException -> handleValidationError(call, cause.cause as ConstraintViolationException)
                else -> {
                    val errorId = UUID.randomUUID()
                    LOGGER.warn(errorId.toString(), cause)
                    LOGGER.warn(errorId.toString(), call.request.headers["User-Agent"])
                    val problem = Problem(
                            title = "Feil ved prosessering av JSON-dataene som ble oppgitt",
                            detail = cause.message,
                            instance = URI.create("urn:sporenstreks:json-mapping-error:$errorId")
                    )
                    call.respond(HttpStatusCode.BadRequest, problem)
                }
            }
        }

        exception<ConstraintViolationException> { cause ->
            handleValidationError(call, cause)
        }
    }

    nais()

    localCookieDispenser(config)

    routing {
        authenticate {
            sporenstreks(get(), get(), get())
        }
    }
}