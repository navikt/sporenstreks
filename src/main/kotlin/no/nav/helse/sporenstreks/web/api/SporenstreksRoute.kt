package no.nav.helse.sporenstreks.web.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.application.ApplicationCall
import io.ktor.application.application
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.readAllParts
import io.ktor.http.content.streamProvider
import io.ktor.request.receive
import io.ktor.request.receiveMultipart
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.response.respondBytes
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.util.KtorExperimentalAPI
import io.ktor.util.pipeline.PipelineContext
import no.nav.helse.sporenstreks.auth.AuthorizationsRepository
import no.nav.helse.sporenstreks.auth.Authorizer
import no.nav.helse.sporenstreks.auth.altinn.AltinnBrukteForLangTidException
import no.nav.helse.sporenstreks.auth.hentIdentitetsnummerFraLoginToken
import no.nav.helse.sporenstreks.auth.hentUtløpsdatoFraLoginToken
import no.nav.helse.sporenstreks.db.RefusjonskravRepository
import no.nav.helse.sporenstreks.domene.Refusjonskrav
import no.nav.helse.sporenstreks.excel.ExcelBulkService
import no.nav.helse.sporenstreks.excel.ExcelParser
import no.nav.helse.sporenstreks.metrics.INNKOMMENDE_REFUSJONSKRAV_BELOEP_COUNTER
import no.nav.helse.sporenstreks.metrics.INNKOMMENDE_REFUSJONSKRAV_COUNTER
import no.nav.helse.sporenstreks.metrics.REQUEST_TIME
import no.nav.helse.sporenstreks.service.RefusjonskravService
import no.nav.helse.sporenstreks.system.AppEnv
import no.nav.helse.sporenstreks.system.getEnvironment
import no.nav.helse.sporenstreks.web.dto.PostListResponseDto
import no.nav.helse.sporenstreks.web.dto.RefusjonskravDto
import no.nav.helse.sporenstreks.web.dto.validation.ValidationProblemDetail
import no.nav.helse.sporenstreks.web.dto.validation.getContextualMessage
import org.koin.ktor.ext.get
import org.valiktor.ConstraintViolationException
import java.io.IOException
import javax.ws.rs.ForbiddenException

private val excelContentType = ContentType.parse("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")

@KtorExperimentalAPI
fun Route.sporenstreks(authorizer: Authorizer, authRepo: AuthorizationsRepository, refusjonskravService: RefusjonskravService) {
    route("api/v1") {

        route("/login-expiry") {
            get {
                call.respond(HttpStatusCode.OK, hentUtløpsdatoFraLoginToken(application.environment.config, call.request))
            }
        }

        route("/refusjonskrav") {
            post("/") {
                val timer = REQUEST_TIME.startTimer()
                try {
                    val refusjonskrav = call.receive<RefusjonskravDto>()
                    authorize(authorizer, refusjonskrav.virksomhetsnummer)

                    val domeneKrav = Refusjonskrav(
                            refusjonskrav.identitetsnummer,
                            refusjonskrav.virksomhetsnummer,
                            refusjonskrav.perioder
                    )
                    val saved = refusjonskravService.saveKravWithKvittering(domeneKrav)
                    call.respond(HttpStatusCode.OK, saved)
                    INNKOMMENDE_REFUSJONSKRAV_COUNTER.inc()
                    INNKOMMENDE_REFUSJONSKRAV_BELOEP_COUNTER.inc(refusjonskrav.perioder.sumByDouble { it.beloep }.div(1000))
                } finally {
                    timer.observeDuration()
                }
            }
            get("/virksomhet/{virksomhetsnummer}") {
                val virksomhetsnummer = requireNotNull(call.parameters["virksomhetsnummer"])
                authorize(authorizer, virksomhetsnummer)
                call.respond(HttpStatusCode.OK, refusjonskravService.getAllForVirksomhet(virksomhetsnummer)
                        .map {
                            RefusjonskravDto(it.identitetsnummer,
                                    it.virksomhetsnummer,
                                    it.perioder)
                        })
            }

            post("/list") {
                val refusjonskravJson = call.receiveText()
                val om = application.get<ObjectMapper>()
                val jsonTree = om.readTree(refusjonskravJson)
                val responseBody = ArrayList<PostListResponseDto>(jsonTree.size())
                val domeneListe = mutableListOf<Refusjonskrav>()

                for (i in 0 until jsonTree.size()) {
                    try {
                        val dto = om.readValue<RefusjonskravDto>(jsonTree[i].traverse())
                        authorize(authorizer, dto.virksomhetsnummer)
                        domeneListe.add(Refusjonskrav(
                                dto.identitetsnummer,
                                dto.virksomhetsnummer,
                                dto.perioder
                        ))
                    } catch (forbiddenEx: ForbiddenException) {
                        responseBody.add(PostListResponseDto(status = PostListResponseDto.Status.GENERIC_ERROR, genericMessage = "Ingen tilgang til virksomheten"))
                    } catch (validationEx: ConstraintViolationException) {
                        val problems = validationEx.constraintViolations.map {
                            ValidationProblemDetail(it.constraint.name, it.getContextualMessage(), it.property, it.value)
                        }
                        responseBody.add(PostListResponseDto(status = PostListResponseDto.Status.VALIDATION_ERRORS, validationErrors = problems))
                    } catch (genericEx: Exception) {
                        if (genericEx.cause is ConstraintViolationException) {
                            val problems = (genericEx.cause as ConstraintViolationException).constraintViolations.map {
                                ValidationProblemDetail(it.constraint.name, it.getContextualMessage(), it.property, it.value)
                            }
                            responseBody.add(PostListResponseDto(status = PostListResponseDto.Status.VALIDATION_ERRORS, validationErrors = problems))
                        } else {
                            responseBody.add(PostListResponseDto(status = PostListResponseDto.Status.GENERIC_ERROR, genericMessage = genericEx.message))
                        }
                    }
                }
                val savedList = refusjonskravService.saveKravListWithKvittering(domeneListe)
                savedList.forEach {
                    INNKOMMENDE_REFUSJONSKRAV_COUNTER.inc()
                    INNKOMMENDE_REFUSJONSKRAV_BELOEP_COUNTER.inc(it.perioder.sumByDouble { it.beloep }.div(1000))
                    responseBody.add(PostListResponseDto(status = PostListResponseDto.Status.OK, referenceNumber = "${it.referansenummer}"))
                }
                call.respond(HttpStatusCode.OK, responseBody)
            }
        }

        route("/bulk") {

            get("/template") {
                val template = javaClass.getResourceAsStream("/bulk-upload/sykepengerefusjon_mal_v08-05-2020.xlsx")
                call.response.headers.append("Content-Disposition", "attachment; filename=\"koronasykepenger_nav.xlsx\"")
                call.respondBytes(template.readAllBytes(), excelContentType)
            }

            post("/upload") {

                val id = hentIdentitetsnummerFraLoginToken(application.environment.config, call.request)
                val multipart = call.receiveMultipart()

                val fileItem = multipart.readAllParts()
                        .filterIsInstance<PartData.FileItem>()
                        .firstOrNull()
                        ?: throw IllegalArgumentException()

                val maxUploadSize = 250 * 1024

                val bytes = fileItem.streamProvider().readNBytes(maxUploadSize + 1)

                if (bytes.size > maxUploadSize) {
                    throw IOException("Den opplastede filen er for stor")
                }

              ExcelBulkService(refusjonskravService, ExcelParser(authorizer))
                        .processExcelFile(bytes.inputStream(), id)

                call.respond(HttpStatusCode.OK, "Søknaden er mottatt.")
            }
        }


        route("/arbeidsgivere") {
            get("/") {
                val id = hentIdentitetsnummerFraLoginToken(application.environment.config, call.request)
                try {
                    val rettigheter = authRepo.hentOrgMedRettigheterForPerson(id)

                    call.respond(rettigheter)
                } catch (ae: AltinnBrukteForLangTidException) {
                    // Midlertidig fiks for å la klienten prøve igjen når noe timer ut ifbm dette kallet til Altinn
                    call.respond(HttpStatusCode.ExpectationFailed)
                }
            }
        }
    }
}

@KtorExperimentalAPI
private fun PipelineContext<Unit, ApplicationCall>.authorize(authorizer: Authorizer, arbeidsgiverId: String) {
    val identitetsnummer = hentIdentitetsnummerFraLoginToken(application.environment.config, call.request)
    if (!authorizer.hasAccess(identitetsnummer, arbeidsgiverId)) {
        throw ForbiddenException()
    }
}