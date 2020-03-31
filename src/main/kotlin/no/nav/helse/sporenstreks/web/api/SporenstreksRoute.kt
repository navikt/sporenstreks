package no.nav.helse.sporenstreks.web.api

import io.ktor.application.ApplicationCall
import io.ktor.application.application
import io.ktor.application.call
import io.ktor.config.ApplicationConfig
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.response.respondText
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
import no.nav.helse.sporenstreks.db.RefusjonskravRepository
import no.nav.helse.sporenstreks.domene.Arbeidsgiverperiode
import no.nav.helse.sporenstreks.domene.Refusjonskrav
import no.nav.helse.sporenstreks.domene.RefusjonskravStatus
import no.nav.helse.sporenstreks.integrasjon.OppgaveService
import no.nav.helse.sporenstreks.integrasjon.rest.LeaderElection.LeaderElectionConsumer
import no.nav.helse.sporenstreks.integrasjon.rest.aktor.AktorConsumer
import no.nav.helse.sporenstreks.metrics.INNKOMMENDE_REFUSJONSKRAV_BELOEP_COUNTER
import no.nav.helse.sporenstreks.metrics.INNKOMMENDE_REFUSJONSKRAV_COUNTER
import no.nav.helse.sporenstreks.metrics.REQUEST_TIME
import no.nav.helse.sporenstreks.utils.MDCOperations
import no.nav.helse.sporenstreks.web.dto.RefusjonskravDto
import org.koin.ktor.ext.getKoin
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

                    val opprettetAv = hentIdentitetsnummerFraLoginToken(application.environment.config, call.request)
                    val domeneKrav = Refusjonskrav(
                            opprettetAv,
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
fun Route.apiTest(config: ApplicationConfig) {
    route("apitest/v1") {

        route("/aktørId") {
            get("/") {
                if (config.property("koin.profile").getString() == "PREPROD") {
                    val aktorConsumer = application.getKoin().get<AktorConsumer>()
                    call.respondText(aktorConsumer.getAktorId(call.request.queryParameters["identitetsnummer"]!!, MDCOperations.generateCallId()))
                }
            }
        }

        get("/isLeader") {
            val leaderElectionConsumer = application.getKoin().get<LeaderElectionConsumer>()
            call.respondText(leaderElectionConsumer.isLeader().toString())
        }

        route("/oppgave") {
            post("/") {
                if (config.property("koin.profile").getString() != "PROD") {
                    val service = application.getKoin().get<OppgaveService>();
                    val aktorConsumer = application.getKoin().get<AktorConsumer>()
                    val refusjonskrav = Refusjonskrav(
                            "20015001543",
                            "26058721211",
                            "123456785",
                            setOf(
                                    Arbeidsgiverperiode(
                                            LocalDate.of(2020, 4, 1),
                                            LocalDate.of(2020, 4, 6),
                                            3, 1000.0
                                    ), Arbeidsgiverperiode(
                                    LocalDate.of(2020, 4, 10),
                                    LocalDate.of(2020, 4, 12),
                                    3, 1000.0
                            )),
                            RefusjonskravStatus.MOTTATT
                    )

                    val aktørId = aktorConsumer.getAktorId(refusjonskrav.identitetsnummer, MDCOperations.generateCallId())
                    val id = service.opprettOppgave(refusjonskrav, "test", aktørId, MDCOperations.generateCallId())
                    call.respond(HttpStatusCode.Accepted, "Opprettet oppgave id=$id")
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