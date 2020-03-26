package no.nav.helse.sporenstreks.web.api

import io.ktor.application.ApplicationCall
import io.ktor.application.application
import io.ktor.application.call
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
import no.nav.helse.sporenstreks.db.PostgresRefusjonskravRepository
import no.nav.helse.sporenstreks.db.RefusjonskravRepository
import no.nav.helse.sporenstreks.domene.Refusjonskrav
import no.nav.helse.sporenstreks.web.dto.RefusjonskravDto
import javax.ws.rs.ForbiddenException

@KtorExperimentalAPI
fun Route.sporenstreks(authorizer: Authorizer, authRepo: AuthorizationsRepository, db: RefusjonskravRepository) {
    route("api/v1") {
        route("/refusjonskrav") {
            post("/") {
                val refusjonskrav = call.receive<RefusjonskravDto>()
                authorize(authorizer, refusjonskrav.virksomhetsnummer)

                //TODOs
                //Opprett PDF
                //Journalfør dokument
                //Opprett sak..?
                //Knytt dokument til sak?
                //Opprett oppgave i gosys
                //Skal det publiseres noe annet sted..?

                val joarkReferanse = "vi må ha en ID fra dok-håndtering"
                val oppgavereferanse = "vi må ha en ID fra oppgavehåndtering"
                val opprettetAv = hentIdentitetsnummerFraLoginToken(application.environment.config, call.request)
                val domeneKrav = Refusjonskrav(
                        opprettetAv,
                        refusjonskrav.identitetsnummer,
                        refusjonskrav.virksomhetsnummer,
                        refusjonskrav.perioder,
                        refusjonskrav.beløp,
                        joarkReferanse,
                        oppgavereferanse
                )

                db.insert(domeneKrav)

                println("Backend mottok og lagret $refusjonskrav")
                call.respond(HttpStatusCode.OK)
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

private suspend fun PipelineContext<Unit, ApplicationCall>.authorize(authorizer: Authorizer, arbeidsgiverId: String) {
    val identitetsnummer = hentIdentitetsnummerFraLoginToken(application.environment.config, call.request)
    if (!authorizer.hasAccess(identitetsnummer, arbeidsgiverId)) {
        throw ForbiddenException()
    }
}