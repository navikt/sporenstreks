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
import no.nav.helse.sporenstreks.web.dto.validation.slåSammenPerioder
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.valiktor.ConstraintViolationException
import java.time.LocalDate
import java.time.LocalDate.of
import java.time.LocalDateTime

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AaregConstraintsTest {
    @BeforeAll
    fun setup() {
        mockkStatic(Class.forName("java.time.LocalDate").kotlin)
        every { LocalDate.now() } returns LocalDate.parse("2022-01-20")
    }

    class AaregConstraintsKtTest {
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
                LocalDate.of(2021, 1, 15),
                LocalDate.of(2021, 1, 20),
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
            refusjonskravDto.validate(arbeidsForhold)
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
    }

    @Test
    fun `merge fragmented periods`() {
        assertThat(
            slåSammenPerioder(
                listOf(
                    // skal ble merget til 1 periode fra 1.1.21 til 28.2.21
                    Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 29)),
                    Periode(LocalDate.of(2021, 2, 1), LocalDate.of(2021, 2, 13)),
                    Periode(LocalDate.of(2021, 2, 15), LocalDate.of(2021, 2, 28)),

                    // skal bli merget til 1
                    Periode(LocalDate.of(2021, 3, 20), LocalDate.of(2021, 3, 31)),
                    Periode(LocalDate.of(2021, 4, 2), LocalDate.of(2021, 4, 30)),

                    // skal bli merget til 1
                    Periode(LocalDate.of(2021, 7, 1), LocalDate.of(2021, 8, 30)),
                    Periode(LocalDate.of(2021, 9, 1), null),
                )
            )
        ).hasSize(3)

        assertThat(
            slåSammenPerioder(
                listOf(
                    Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 29)),
                    Periode(LocalDate.of(2021, 9, 1), null),
                )
            )
        ).hasSize(2)

        assertThat(
            slåSammenPerioder(
                listOf(
                    Periode(LocalDate.of(2021, 9, 1), null),
                )
            )
        ).hasSize(1)
    }
}
