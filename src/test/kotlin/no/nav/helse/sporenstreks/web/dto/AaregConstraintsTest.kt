package no.nav.helse.sporenstreks.web.dto

import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.every
import io.mockk.mockkStatic
import no.nav.helse.TestData
import no.nav.helse.arbeidsgiver.integrasjoner.aareg.*
import no.nav.helse.arbeidsgiver.utils.loadFromResources
import no.nav.helse.sporenstreks.domene.Arbeidsgiverperiode
import no.nav.helse.sporenstreks.web.dto.validation.AaregPeriode
import no.nav.helse.sporenstreks.web.dto.validation.måHaAktivtArbeidsforholdEnkel
import no.nav.helse.sporenstreks.web.dto.validation.slåSammenPerioder
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.valiktor.ConstraintViolationException
import org.valiktor.functions.validateForEach
import org.valiktor.validate
import java.time.LocalDate
import java.time.LocalDate.MAX
import java.time.LocalDate.of
import java.time.LocalDateTime

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AaregConstraintsTest {
    @BeforeAll
    fun setup() {
        mockkStatic(Class.forName("java.time.LocalDate").kotlin)
        every { LocalDate.now() } returns LocalDate.parse("2022-01-20")
    }

    @Test
    @Disabled
    fun `Rådata fra aareg (Brukes for å feilsøke med respons fra AA-reg)`() {
        val om = ObjectMapper()
        om.registerModule(KotlinModule())
        om.registerModule(Jdk8Module())
        om.registerModule(JavaTimeModule())
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        om.configure(SerializationFeature.INDENT_OUTPUT, true)
        om.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

        om.setDefaultPrettyPrinter(
            DefaultPrettyPrinter().apply {
                indentArraysWith(DefaultPrettyPrinter.FixedSpaceIndenter.instance)
                indentObjectsWith(DefaultIndenter("  ", "\n"))
            }
        )

        // Legg aareg JSON-respons i src/test/resources/aareg.json
        val aaregFile = "aareg.json".loadFromResources()
        val arbeidsforhold = om.readValue<List<Arbeidsforhold>>(aaregFile)
            // Legg inn organisasjonsnummer
            .filter { it.arbeidsgiver.organisasjonsnummer == "XXXXXXXX" }

        // Endre til perioden kravet gjelder
        val arbeidsgiverPeriode = Arbeidsgiverperiode(
            of(2021, 1, 15),
            of(2021, 1, 20),
            4,
            beloep = 2.0
        )

        val refusjonskravDto = RefusjonskravDto(
            TestData.validIdentitetsnummer,
            TestData.validOrgNr,
            setOf(
                arbeidsgiverPeriode
            )
        )
        refusjonskravDto.validate(arbeidsforhold)
    }

    @Test
    fun `Gyldig arbeidsforhold og gyldig krav`() {
        val arbeidsgiver = Arbeidsgiver("AS", "123456785")
        val opplysningspliktig = Opplysningspliktig("AS", "1212121212")
        val arbeidsForhold = listOf(
            Arbeidsforhold(
                arbeidsgiver,
                opplysningspliktig,
                emptyList(),
                Ansettelsesperiode(
                    Periode(
                        of(2021, 1, 1),
                        null
                    )
                ),
                LocalDateTime.now()
            )
        )

        RefusjonskravDto(
            TestData.validIdentitetsnummer,
            TestData.validOrgNr,
            setOf(
                Arbeidsgiverperiode(
                    of(2022, 1, 1),
                    of(2022, 1, 10),
                    2, 2.3
                )
            )
        ).validate(arbeidsForhold)
    }

    @Test
    fun `Ikke aktivt arbeidsforhold i krav perioden`() {
        val arbeidsgiver = Arbeidsgiver("AS", "123456785")
        val opplysningspliktig = Opplysningspliktig("AS", "1212121212")
        val arbeidsForhold = listOf(
            Arbeidsforhold(
                arbeidsgiver,
                opplysningspliktig,
                emptyList(),
                Ansettelsesperiode(
                    Periode(
                        of(2021, 1, 1),
                        of(2021, 1, 3),
                    )
                ),
                LocalDateTime.now()
            )
        )

        val refusjonskravDto = RefusjonskravDto(
            TestData.validIdentitetsnummer,
            TestData.validOrgNr,
            setOf(
                Arbeidsgiverperiode(
                    of(2022, 1, 1),
                    of(2022, 1, 7),
                    2, 2.3
                )
            )
        )
        Assertions.assertThatExceptionOfType(ConstraintViolationException::class.java).isThrownBy {
            refusjonskravDto.validate(arbeidsForhold)
        }
    }

    @Test
    fun `Ikke aktivt arbeidsforhold i denne organisasjonen`() {
        val arbeidsgiver = Arbeidsgiver("AS", "810007842")
        val opplysningspliktig = Opplysningspliktig("AS", "1212121212")
        val arbeidsForhold = listOf(
            Arbeidsforhold(
                arbeidsgiver,
                opplysningspliktig,
                emptyList(),
                Ansettelsesperiode(
                    Periode(
                        of(2021, 1, 1),
                        null
                    )
                ),
                LocalDateTime.now()
            )
        )

        val refusjonskravDto = RefusjonskravDto(
            TestData.validIdentitetsnummer,
            TestData.validOrgNr,
            setOf(
                Arbeidsgiverperiode(
                    of(2022, 1, 1),
                    of(2022, 1, 7),
                    2, 2.3
                )
            )
        )
        Assertions.assertThatExceptionOfType(ConstraintViolationException::class.java).isThrownBy {
            refusjonskravDto.validate(arbeidsForhold)
        }
    }

    @Test
    fun `Ansatt slutter fram i tid`() {
        val periode = Arbeidsgiverperiode(
            LocalDate.of(2021, 1, 15),
            LocalDate.of(2021, 1, 20),
            4,
            beloep = 2590.8,
        )

        validate(periode) {
            validate(Arbeidsgiverperiode::fom).måHaAktivtArbeidsforholdEnkel(periode, TestData.validOrgNr, TestData.arbeidsforholdMedSluttDato)
        }
    }

    @Test
    fun `Refusjonskravet er innenfor Arbeidsforholdet`() {
        val periode = Arbeidsgiverperiode(
            LocalDate.of(2021, 1, 15),
            LocalDate.of(2021, 1, 18),
            2,
            beloep = 2590.8,
        )

        validate(periode) {
            validate(Arbeidsgiverperiode::fom).måHaAktivtArbeidsforholdEnkel(periode, TestData.validOrgNr, TestData.evigArbeidsForholdListe)
        }
    }

    @Test
    fun `Sammenehengende arbeidsforhold slås sammen til en periode`() {

        val arbeidsForhold1 = Arbeidsforhold(
            TestData.arbeidsgiver,
            TestData.opplysningspliktig,
            emptyList(),
            Ansettelsesperiode(
                Periode(
                    LocalDate.of(2019, 1, 1),
                    LocalDate.of(2021, 2, 28)
                )
            ),
            LocalDateTime.now()
        )

        val arbeidsForhold2 = Arbeidsforhold(
            TestData.arbeidsgiver,
            TestData.opplysningspliktig,
            emptyList(),
            Ansettelsesperiode(
                Periode(
                    LocalDate.of(2021, 3, 1),
                    null
                )
            ),
            LocalDateTime.now()
        )

        val refusjonskravDto = RefusjonskravDto(
            TestData.validIdentitetsnummer,
            TestData.validOrgNr,
            setOf(
                Arbeidsgiverperiode(
                    LocalDate.of(2021, 1, 15),
                    LocalDate.of(2021, 1, 18),
                    2,
                    beloep = 2590.8,
                ),
                Arbeidsgiverperiode(
                    LocalDate.of(2021, 2, 26),
                    LocalDate.of(2021, 3, 10),
                    12,
                    beloep = 2590.8,
                )
            )
        )

        validate(refusjonskravDto) {
            validate(RefusjonskravDto::perioder).validateForEach {
                validate(Arbeidsgiverperiode::fom).måHaAktivtArbeidsforholdEnkel(
                    it,
                    TestData.validOrgNr,
                    listOf(arbeidsForhold1, arbeidsForhold2)
                )
            }
        }
    }

    @Test
    fun `Refusjonsdato er før Arbeidsforhold har begynt`() {

        val periode = Arbeidsgiverperiode(
            LocalDate.of(2021, 1, 1),
            LocalDate.of(2021, 1, 5),
            2,
            beloep = 2590.8,
        )
        validationShouldFailFor(Arbeidsgiverperiode::fom) {
            validate(periode) {
                validate(Arbeidsgiverperiode::fom).måHaAktivtArbeidsforholdEnkel(
                    periode,
                    TestData.validOrgNr,
                    TestData.pågåendeArbeidsforholdListe
                )
            }
        }
    }

    @Test
    fun `Refusjonsdato etter Arbeidsforhold er avsluttet`() {
        val periode = Arbeidsgiverperiode(
            LocalDate.of(2021, 5, 15),
            LocalDate.of(2021, 5, 18),
            2,
            beloep = 2590.8,
        )

        validationShouldFailFor(Arbeidsgiverperiode::fom) {
            validate(periode) {
                validate(Arbeidsgiverperiode::fom).måHaAktivtArbeidsforholdEnkel(
                    periode,
                    TestData.validOrgNr,
                    TestData.avsluttetArbeidsforholdListe
                )
            }
        }
    }

    @Test
    fun `merge fragmented periods`() {
        assertThat(
            slåSammenPerioder(
                mutableListOf(
                    // skal ble merget til 1 periode fra 1.1.21 til 28.2.21
                    AaregPeriode(of(2021, 1, 1), of(2021, 1, 29)),
                    AaregPeriode(of(2021, 2, 1), of(2021, 2, 13)),
                    AaregPeriode(of(2021, 2, 15), of(2021, 2, 28)),

                    // skal bli merget til 1
                    AaregPeriode(of(2021, 3, 20), of(2021, 3, 31)),
                    AaregPeriode(of(2021, 4, 2), of(2021, 4, 30)),

                    // skal bli merget til 1
                    AaregPeriode(of(2021, 7, 1), of(2021, 8, 30)),
                    AaregPeriode(of(2021, 9, 1), MAX),
                )
            )
        ).hasSize(3)

        assertThat(
            slåSammenPerioder(
                mutableListOf(
                    AaregPeriode(of(2021, 1, 1), of(2021, 1, 29)),
                    AaregPeriode(of(2021, 9, 1), MAX),
                )
            )
        ).hasSize(2)

        assertThat(
            slåSammenPerioder(
                mutableListOf(
                    AaregPeriode(of(2021, 9, 1), MAX),
                )
            )
        ).hasSize(1)

        assertThat(
            slåSammenPerioder(
                mutableListOf(
                    AaregPeriode(of(2022, 2, 1), MAX),
                    AaregPeriode(of(2005, 12, 1), of(2014, 12, 31)),
                    AaregPeriode(of(1984, 10, 20), of(2000, 10, 25)),
                    AaregPeriode(of(1984, 10, 20), of(1993, 10, 24)),
                    AaregPeriode(of(1984, 10, 20), of(2022, 1, 31)),
                )
            )
        ).hasSize(1)
    }
}
