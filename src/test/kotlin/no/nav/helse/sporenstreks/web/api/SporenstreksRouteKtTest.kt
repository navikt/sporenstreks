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
import io.mockk.every
import io.mockk.mockkStatic
import no.nav.helse.TestData
import no.nav.helse.sporenstreks.domene.Arbeidsgiverperiode
import no.nav.helse.sporenstreks.web.dto.PostListResponseDto
import no.nav.helse.sporenstreks.web.dto.RefusjonskravDto
import no.nav.helse.sporenstreks.web.integration.ControllerIntegrationTestBase
import no.nav.helse.sporenstreks.web.sporenstreksModule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.ktor.ext.get
import java.time.LocalDate

@KtorExperimentalAPI
class SporenstreksRouteKtTest : ControllerIntegrationTestBase() {

    @BeforeEach
    fun setup() {
        mockkStatic(Class.forName("java.time.LocalDate").kotlin)
        every { LocalDate.now() } returns LocalDate.parse("2022-05-01")
    }

    @KtorExperimentalLocationsAPI
    @Test
    fun `Returnerer en tom liste hvis det ikke finnes noe krav`() {
        configuredTestApplication({
            sporenstreksModule()
        }) {
            doAuthenticatedRequest(HttpMethod.Get, "api/v1/refusjonskrav/virksomhet/910098896") {
            }.apply {
                assertThat(response.status()).isEqualTo(HttpStatusCode.OK)
                assertThat(response.content).isEqualTo("[ ]")
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
            val dto = RefusjonskravDto(
                identitetsnummer = TestData.validIdentitetsnummer,
                virksomhetsnummer = "910098896",
                perioder = setOf(
                    Arbeidsgiverperiode(
                        fom = LocalDate.of(2022, 3, 17),
                        tom = LocalDate.of(2022, 3, 25),
                        antallDagerMedRefusjon = 3,
                        beloep = 6000.0
                    )
                )
            )

            doAuthenticatedRequest(HttpMethod.Post, "api/v1/refusjonskrav") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    om.writeValueAsString(
                        dto
                    )
                )
            }.apply {
                assertThat(response.status()).isEqualTo(HttpStatusCode.OK)
            }

            doAuthenticatedRequest(HttpMethod.Get, "api/v1/refusjonskrav/virksomhet/910098896") {
            }.apply {
                assertThat(response.status()).isEqualTo(HttpStatusCode.OK)
                val resultat: List<RefusjonskravDto> = om.readValue(response.content!!)
                assertThat(resultat).isEqualTo(listOf(dto))
            }
        }
    }

    @KtorExperimentalLocationsAPI
    @Test
    fun `Gir ikke 500 selvom alle kravene feiler validering`() {
        configuredTestApplication({
            sporenstreksModule()
        }) {
            val om = application.get<ObjectMapper>()
            val dtoListe = listOf(
                RefusjonskravDtoMock(
                    identitetsnummer = TestData.validIdentitetsnummer,
                    virksomhetsnummer = "910098896",
                    perioder = setOf(
                        Arbeidsgiverperiode(
                            fom = LocalDate.of(2022, 3, 17),
                            tom = LocalDate.of(2022, 3, 25),
                            antallDagerMedRefusjon = 20,
                            beloep = 6000.0
                        )
                    )
                )
            )
            doAuthenticatedRequest(HttpMethod.Post, "api/v1/refusjonskrav/list") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    om.writeValueAsString(
                        dtoListe
                    )
                )
            }.apply {
                assertThat(response.status()).isEqualTo(HttpStatusCode.OK)
                val resultat: List<PostListResponseDto> = om.readValue(response.content!!)
                assertThat(resultat).hasSize(1)
                assertThat(resultat.first().status).isEqualTo(PostListResponseDto.Status.VALIDATION_ERRORS)
            }
        }
    }

    @KtorExperimentalLocationsAPI
    @Test
    fun `Beholder rekkefølgen på valideringer`() {
        configuredTestApplication({
            sporenstreksModule()
        }) {
            val om = application.get<ObjectMapper>()
            val dtoListe = listOf(
                RefusjonskravDtoMock(
                    identitetsnummer = TestData.validIdentitetsnummer,
                    virksomhetsnummer = "910098896",
                    perioder = setOf(
                        Arbeidsgiverperiode(
                            fom = LocalDate.of(2022, 3, 20),
                            tom = LocalDate.of(2022, 3, 25),
                            antallDagerMedRefusjon = 1,
                            beloep = 6000.0
                        )
                    )
                ),
                RefusjonskravDtoMock(
                    identitetsnummer = TestData.validIdentitetsnummer,
                    virksomhetsnummer = "910098896",
                    perioder = setOf(
                        Arbeidsgiverperiode(
                            fom = LocalDate.of(2022, 3, 17),
                            tom = LocalDate.of(2022, 3, 18),
                            antallDagerMedRefusjon = 4,
                            beloep = 6000.0
                        )
                    )
                ),
                RefusjonskravDtoMock(
                    identitetsnummer = TestData.validIdentitetsnummer,
                    virksomhetsnummer = "910098896",
                    perioder = setOf(
                        Arbeidsgiverperiode(
                            fom = LocalDate.of(2022, 3, 27),
                            tom = LocalDate.of(2022, 3, 29),
                            antallDagerMedRefusjon = 1,
                            beloep = 9000.0
                        )
                    )
                ),
                RefusjonskravDtoMock(
                    identitetsnummer = TestData.validIdentitetsnummer,
                    virksomhetsnummer = "910098896",
                    perioder = setOf(
                        Arbeidsgiverperiode(
                            fom = LocalDate.of(2022, 4, 1),
                            tom = LocalDate.of(2022, 4, 3),
                            antallDagerMedRefusjon = 1,
                            beloep = 999999999999.0
                        )
                    )
                )
            )
            doAuthenticatedRequest(HttpMethod.Post, "api/v1/refusjonskrav/list") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    om.writeValueAsString(
                        dtoListe
                    )
                )
            }.apply {
                assertThat(response.status()).isEqualTo(HttpStatusCode.OK)
                val resultat: List<PostListResponseDto> = om.readValue(response.content!!)
                assertThat(resultat).hasSize(4)
                assertThat(resultat[0].status).isEqualTo(PostListResponseDto.Status.VALIDATION_ERRORS)
                assertThat(resultat[0].validationErrors).isNull()
                assertThat(resultat[1].status).isEqualTo(PostListResponseDto.Status.VALIDATION_ERRORS)
                assertThat(resultat[1].validationErrors?.map { it.validationType })
                    .contains("RefusjonsdagerKanIkkeOverstigePeriodelengdenConstraint")
                assertThat(resultat[2].status).isEqualTo(PostListResponseDto.Status.VALIDATION_ERRORS)
                assertThat(resultat[2].validationErrors?.map { it.validationType })
                    .contains("RefusjonsDagerConstraint")
                assertThat(resultat[3].status).isEqualTo(PostListResponseDto.Status.VALIDATION_ERRORS)
                assertThat(resultat[3].validationErrors?.map { it.validationType }).contains("LessOrEqual")
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
                assertThat(response.status()).isEqualTo(HttpStatusCode.Unauthorized)
            }
        }
    }

    @KtorExperimentalLocationsAPI
    @Test
    fun `Alle krav får valideringsfeil hvis en har det`() {
        configuredTestApplication({
            sporenstreksModule()
        }) {
            val om = application.get<ObjectMapper>()
            val dtoListe = listOf(
                RefusjonskravDtoMock(
                    identitetsnummer = TestData.validIdentitetsnummer,
                    virksomhetsnummer = "910098896",
                    perioder = setOf(
                        Arbeidsgiverperiode(
                            fom = LocalDate.of(2022, 1, 12),
                            tom = LocalDate.of(2022, 1, 20),
                            antallDagerMedRefusjon = 3,
                            beloep = 50.0
                        )
                    )
                ),
                RefusjonskravDtoMock(
                    identitetsnummer = TestData.validIdentitetsnummer,
                    virksomhetsnummer = "910098896",
                    perioder = setOf(
                        Arbeidsgiverperiode(
                            fom = LocalDate.of(2022, 1, 12),
                            tom = LocalDate.of(2022, 1, 20),
                            antallDagerMedRefusjon = 3,
                            beloep = 50.0
                        )
                    )
                ),
                RefusjonskravDtoMock(
                    identitetsnummer = TestData.validIdentitetsnummer,
                    virksomhetsnummer = "910098896",
                    perioder = setOf(
                        Arbeidsgiverperiode(
                            fom = LocalDate.of(2022, 1, 12),
                            tom = LocalDate.of(2022, 1, 20),
                            antallDagerMedRefusjon = 3,
                            beloep = 50.0
                        )
                    )
                ),
                RefusjonskravDtoMock(
                    identitetsnummer = TestData.validIdentitetsnummer,
                    virksomhetsnummer = "910098896",
                    perioder = setOf(
                        Arbeidsgiverperiode(
                            fom = LocalDate.of(2022, 1, 12),
                            tom = LocalDate.of(2022, 1, 20),
                            antallDagerMedRefusjon = 10,
                            beloep = 50.0
                        )
                    )
                )
            )
            doAuthenticatedRequest(HttpMethod.Post, "api/v1/refusjonskrav/list") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    om.writeValueAsString(
                        dtoListe
                    )
                )
            }.apply {
                assertThat(response.status()).isEqualTo(HttpStatusCode.OK)
                val resultat: List<PostListResponseDto> = om.readValue(response.content!!)
                assertThat(resultat).hasSize(4)
                assertThat(resultat[0].status).isEqualTo(PostListResponseDto.Status.VALIDATION_ERRORS)
                assertThat(resultat[1].status).isEqualTo(PostListResponseDto.Status.VALIDATION_ERRORS)
                assertThat(resultat[2].status).isEqualTo(PostListResponseDto.Status.VALIDATION_ERRORS)
                assertThat(resultat[3].status).isEqualTo(PostListResponseDto.Status.VALIDATION_ERRORS)
            }
        }
    }

    @KtorExperimentalLocationsAPI
    @Test
    fun `Gir riktig feilmedling uten gyldig arbeidsforhold`() {
        configuredTestApplication({
            sporenstreksModule()
        }) {
            val om = application.get<ObjectMapper>()
            val dtoListe = listOf(
                RefusjonskravDtoMock(
                    identitetsnummer = TestData.validIdentitetsnummer,
                    virksomhetsnummer = "805824352",
                    perioder = setOf(
                        Arbeidsgiverperiode(
                            fom = LocalDate.of(2022, 3, 12),
                            tom = LocalDate.of(2022, 3, 25),
                            antallDagerMedRefusjon = 7,
                            beloep = 6000.0
                        )
                    )
                )
            )
            doAuthenticatedRequest(HttpMethod.Post, "api/v1/refusjonskrav/list") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    om.writeValueAsString(
                        dtoListe
                    )
                )
            }.apply {
                assertThat(response.status()).isEqualTo(HttpStatusCode.OK)
                val resultat: List<PostListResponseDto> = om.readValue(response.content!!)
                assertThat(resultat).hasSize(1)
                assertThat(resultat.first().status).isEqualTo(PostListResponseDto.Status.VALIDATION_ERRORS)
                assertThat(resultat[0].validationErrors?.map { it.validationType })
                    .contains("ArbeidsforholdConstraint")
            }
        }
    }
}

data class RefusjonskravDtoMock(
    val identitetsnummer: String,
    val virksomhetsnummer: String,
    val perioder: Set<Arbeidsgiverperiode>
)
