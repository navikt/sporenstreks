package no.nav.helse.sporenstreks.web.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.TestData
import no.nav.helse.sporenstreks.domene.Arbeidsgiverperiode
import no.nav.helse.sporenstreks.web.dto.RefusjonskravDto
import no.nav.helse.sporenstreks.web.integration.ControllerIntegrationTestBase
import no.nav.helse.sporenstreks.web.sporenstreksModule
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.koin.ktor.ext.get
import java.time.LocalDate

@KtorExperimentalAPI
class SporenstreksRouteKtTest : ControllerIntegrationTestBase() {

    @KtorExperimentalLocationsAPI
    @Test
    fun `Returnerer en tom liste hvis det ikke finnes noe krav`() {
        configuredTestApplication({
            sporenstreksModule()
        }) {
            doAuthenticatedRequest(HttpMethod.Get, "api/v1/refusjonskrav/virksomhet/910020102") {
            }.apply {
                Assertions.assertThat(response.status()).isEqualTo(HttpStatusCode.OK)
                Assertions.assertThat(response.content).isEqualTo("[ ]")
            }
        }
    }

    @KtorExperimentalLocationsAPI
    @Test
    fun `Lagrer et krav og returnerer det i liste`() {
        configuredTestApplication({
            sporenstreksModule()
        }) {
            val om = application.get<ObjectMapper>()
            val dto = RefusjonskravDto(identitetsnummer = TestData.validIdentitetsnummer,
                    virksomhetsnummer = "910020102",
                    perioder = setOf(Arbeidsgiverperiode(
                            fom = LocalDate.of(2020, 3, 17),
                            tom = LocalDate.of(2020, 3, 25),
                            antallDagerMedRefusjon = 3,
                            beloep = 6000.0
                    ))
            )
            doAuthenticatedRequest(HttpMethod.Post, "api/v1/refusjonskrav") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                        om.writeValueAsString(
                                dto)
                )
            }.apply {
                Assertions.assertThat(response.status()).isEqualTo(HttpStatusCode.OK)
            }


            doAuthenticatedRequest(HttpMethod.Get, "api/v1/refusjonskrav/virksomhet/910020102") {
            }.apply {
                Assertions.assertThat(response.status()).isEqualTo(HttpStatusCode.OK)
                val resultat: List<RefusjonskravDto> = om.readValue(response.content!!)
                Assertions.assertThat(resultat).isEqualTo(listOf(dto))
            }
        }

    }

    @KtorExperimentalLocationsAPI
    @Test
    fun `Returnerer 401 hvis ikke autentisert`() {
        configuredTestApplication({
            sporenstreksModule()
        }) {
            handleRequest(HttpMethod.Get, "api/v1/refusjonskrav/virksomhet/${TestData.validOrgNr}") {
            }.apply {
                Assertions.assertThat(response.status()).isEqualTo(HttpStatusCode.Unauthorized)
            }
        }
    }

}