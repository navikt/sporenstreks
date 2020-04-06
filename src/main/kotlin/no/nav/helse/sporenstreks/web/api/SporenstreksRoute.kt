package no.nav.helse.sporenstreks.web.api

import io.ktor.application.ApplicationCall
import io.ktor.application.application
import io.ktor.application.call
import io.ktor.config.ApplicationConfig
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.readAllParts
import io.ktor.http.content.streamProvider
import io.ktor.request.receive
import io.ktor.request.receiveMultipart
import io.ktor.response.respond
import io.ktor.response.respondBytes
import io.ktor.response.respondText
import io.ktor.response.respondFile
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
import no.nav.helse.sporenstreks.excel.ExcelBulkService
import no.nav.helse.sporenstreks.excel.ExcelParser
import no.nav.helse.sporenstreks.db.RefusjonskravRepository
import no.nav.helse.sporenstreks.domene.Refusjonskrav
import no.nav.helse.sporenstreks.metrics.INNKOMMENDE_REFUSJONSKRAV_BELOEP_COUNTER
import no.nav.helse.sporenstreks.metrics.INNKOMMENDE_REFUSJONSKRAV_COUNTER
import no.nav.helse.sporenstreks.metrics.REQUEST_TIME
import no.nav.helse.sporenstreks.metrics.TEST_COUNTER
import no.nav.helse.sporenstreks.system.AppEnv
import no.nav.helse.sporenstreks.system.getEnvironment
import no.nav.helse.sporenstreks.utils.MDCOperations
import no.nav.helse.sporenstreks.web.dto.RefusjonskravDto
import org.koin.ktor.ext.getKoin
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.IllegalArgumentException
import java.time.LocalDate
import javax.ws.rs.ForbiddenException

@KtorExperimentalAPI
fun Route.sporenstreks(authorizer: Authorizer, authRepo: AuthorizationsRepository, db: RefusjonskravRepository) {
    route("api/v1") {
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

                    val saved = db.insert(domeneKrav)
                    call.respond(HttpStatusCode.OK, saved)
                    INNKOMMENDE_REFUSJONSKRAV_COUNTER.inc()
                    INNKOMMENDE_REFUSJONSKRAV_BELOEP_COUNTER.inc(refusjonskrav.perioder.sumByDouble { it.beloep }.div(1000))
                } finally {
                    timer.observeDuration()
                }
            }
        }

        route("/bulk") {

            get("/template") {
                val template = javaClass.getResource("/bulk-upload/koronasykepengerefusjon_nav.xlsx").file
                call.respondFile(File(template))
            }

            post("/upload") {
                if (application.environment.config.getEnvironment() == AppEnv.PROD) {
                    call.respond(HttpStatusCode.NotFound, "")
                    return@post
                }

                val id = hentIdentitetsnummerFraLoginToken(application.environment.config, call.request)
                val multipart = call.receiveMultipart()
                val fileItem = multipart.readAllParts() // TODO: Hånedhev en grense på 100KB
                        .filterIsInstance<PartData.FileItem>()
                        .firstOrNull()
                        ?: throw IllegalArgumentException()


                val resultingFile = ExcelBulkService(db, ExcelParser(authorizer))
                        .processExcelFile(fileItem.streamProvider(), id)

                call.response.headers.append("Content-Disposition", "attachment; filename=\"koronasykepenger_kvittering.xlsx\"")

                call.respondBytes(
                        resultingFile,
                        ContentType.parse("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
                        HttpStatusCode.OK
                )
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

private fun PipelineContext<Unit, ApplicationCall>.authorize(authorizer: Authorizer, arbeidsgiverId: String) {
    val identitetsnummer = hentIdentitetsnummerFraLoginToken(application.environment.config, call.request)
    if (!authorizer.hasAccess(identitetsnummer, arbeidsgiverId)) {
        throw ForbiddenException()
    }
}