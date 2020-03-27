package no.nav.helse.sporenstreks.web.api

import io.ktor.application.ApplicationCall
import io.ktor.application.application
import io.ktor.application.call
import io.ktor.config.ApplicationConfig
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.util.KtorExperimentalAPI
import io.ktor.util.pipeline.PipelineContext
import no.nav.helse.sporenstreks.auth.AuthorizationsRepository
import no.nav.helse.sporenstreks.auth.Authorizer
import no.nav.helse.sporenstreks.auth.hentIdentitetsnummerFraLoginToken
import no.nav.helse.sporenstreks.db.RefusjonskravRepository
import no.nav.helse.sporenstreks.domene.Arbeidsgiverperiode
import no.nav.helse.sporenstreks.domene.Refusjonskrav
import no.nav.helse.sporenstreks.integrasjon.JoarkService
import no.nav.helse.sporenstreks.metrics.INNKOMMENDE_REFUSJONSKRAV_COUNTER
import no.nav.helse.sporenstreks.metrics.REQUEST_TIME
import no.nav.helse.sporenstreks.web.dto.RefusjonskravDto
import java.time.LocalDate
import javax.ws.rs.ForbiddenException

@KtorExperimentalAPI
fun Route.sporenstreks(authorizer: Authorizer, authRepo: AuthorizationsRepository, db: RefusjonskravRepository) {
    route("api/v1") {
        route("/refusjonskrav") {
            post("/") {
                INNKOMMENDE_REFUSJONSKRAV_COUNTER.inc()
                val timer = REQUEST_TIME.startTimer()
                try {
                    val refusjonskrav = call.receive<RefusjonskravDto>()
                    authorize(authorizer, refusjonskrav.virksomhetsnummer)

                    val opprettetAv = hentIdentitetsnummerFraLoginToken(application.environment.config, call.request)
                    val domeneKrav = Refusjonskrav(
                            opprettetAv,
                            refusjonskrav.identitetsnummer,
                            refusjonskrav.virksomhetsnummer,
                            refusjonskrav.perioder
                    )

                    val saved = db.insert(domeneKrav)
                    call.respond(HttpStatusCode.OK, saved)
                } finally {
                    timer.observeDuration()
                }
            }
        }

        route("/arbeidsgivere") {
            get("/") {
                val id = hentIdentitetsnummerFraLoginToken(application.environment.config, call.request)
                call.respond(authRepo.hentOrgMedRettigheterForPerson(id))
            }
        }
    }
}

@KtorExperimentalAPI
fun Route.testJournalføring(joarkService: JoarkService, config: ApplicationConfig) {
    route("apitest/v1") {
        get("/journalfør") {
            if (config.property("koin.profile").getString() == "PREPROD") {
                joarkService.journalfør(Refusjonskrav(
                        opprettetAv = "test",
                        identitetsnummer = "08018421659",
                        virksomhetsnummer = "123456785",
                        perioder = setOf(Arbeidsgiverperiode(
                                fom = LocalDate.of(2020, 3, 16),
                                tom = LocalDate.of(2020, 4, 1),
                                antallDagerMedRefusjon = 5,
                                beloep = 5000.0
                        ))
                )
                )
            }
        }
    }
}


private suspend fun PipelineContext<Unit, ApplicationCall>.authorize(authorizer: Authorizer, arbeidsgiverId: String) {
    val identitetsnummer = hentIdentitetsnummerFraLoginToken(application.environment.config, call.request)
    if (!authorizer.hasAccess(identitetsnummer, arbeidsgiverId)) {
        throw ForbiddenException()
    }
}