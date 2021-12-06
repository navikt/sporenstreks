package no.nav.helse.sporenstreks.web.dto

import io.mockk.every
import io.mockk.mockkStatic
import no.nav.helse.TestData
import no.nav.helse.sporenstreks.domene.Arbeidsgiverperiode
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.valiktor.ConstraintViolationException
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RefusjonsKravDtoTest {

    @BeforeAll
    fun setup() {
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns LocalDate.parse("2021-12-06")
    }

    @Test
    fun `Refusjonskrav i ny periode validerer OK`() {
        RefusjonskravDto(
            TestData.validIdentitetsnummer,
            TestData.validOrgNr,
            setOf(
                Arbeidsgiverperiode(
                    LocalDate.of(2021, 12, 2),
                    LocalDate.of(2021, 12, 6),
                    2, 2.3
                )
            )
        )
    }

    @Test
    fun `Refusjonskrav i gammel periode validerer OK`() {
        RefusjonskravDto(
            TestData.validIdentitetsnummer,
            TestData.validOrgNr,
            setOf(
                Arbeidsgiverperiode(
                    LocalDate.of(2021, 9, 2),
                    LocalDate.of(2021, 9, 6),
                    2, 2.3
                )
            )
        )
    }

    @Test
    fun `Gyldig refusjonskrav validerer OK`() {
        RefusjonskravDto(
            TestData.validIdentitetsnummer,
            TestData.validOrgNr,
            setOf(
                Arbeidsgiverperiode(
                    LocalDate.of(2021, 9, 18),
                    LocalDate.of(2021, 9, 21),
                    2, 2.3
                ),
                Arbeidsgiverperiode(
                    LocalDate.of(2021, 9, 22),
                    LocalDate.of(2021, 9, 29),
                    4, 2.4
                )
            )
        )
    }

    @Test
    fun `Kant-i-kant perioder er lov`() {
        RefusjonskravDto(
            TestData.validIdentitetsnummer,
            TestData.validOrgNr,
            setOf(
                Arbeidsgiverperiode(
                    LocalDate.of(2021, 9, 16),
                    LocalDate.of(2021, 9, 21),
                    2, 2.0
                ),
                Arbeidsgiverperiode(
                    LocalDate.of(2021, 9, 21),
                    LocalDate.of(2021, 9, 25),
                    4, 2.0
                )
            )
        )
    }

    @Test
    fun `Overlappende perioder er ikke lov`() {
        Assertions.assertThatExceptionOfType(ConstraintViolationException::class.java).isThrownBy {
            RefusjonskravDto(
                TestData.validIdentitetsnummer,
                TestData.validOrgNr,
                setOf(
                    Arbeidsgiverperiode(
                        LocalDate.of(2021, 9, 1),
                        LocalDate.of(2021, 9, 6),
                        2, 1.0
                    ),
                    Arbeidsgiverperiode(
                        LocalDate.of(2021, 9, 5),
                        LocalDate.of(2021, 9, 10),
                        4, 2.0
                    )
                )
            )
        }
    }

    @Test
    fun `Negativt beløp feiler`() {
        Assertions.assertThatExceptionOfType(ConstraintViolationException::class.java).isThrownBy {
            RefusjonskravDto(
                TestData.validIdentitetsnummer,
                TestData.validOrgNr,
                setOf(
                    Arbeidsgiverperiode(
                        LocalDate.of(2021, 9, 1),
                        LocalDate.of(2021, 9, 5),
                        2, -14.65
                    )
                )
            )
        }
    }

    @Test
    fun `Mer enn 16 dager mellom perioder feiler`() {
        Assertions.assertThatExceptionOfType(ConstraintViolationException::class.java).isThrownBy {
            RefusjonskravDto(
                TestData.validIdentitetsnummer,
                TestData.validOrgNr,
                setOf(
                    Arbeidsgiverperiode(
                        LocalDate.of(2021, 9, 1),
                        LocalDate.of(2021, 9, 5),
                        2, 2.0
                    ),
                    Arbeidsgiverperiode(
                        LocalDate.of(2021, 9, 23),
                        LocalDate.of(2021, 9, 29),
                        8, 0.0
                    )
                )
            )
        }
    }

    @Test
    fun `Maks 13 dager med refusjon`() {
        Assertions.assertThatExceptionOfType(ConstraintViolationException::class.java).isThrownBy {
            RefusjonskravDto(
                TestData.validIdentitetsnummer,
                TestData.validOrgNr,
                setOf(
                    Arbeidsgiverperiode(
                        LocalDate.of(2021, 9, 1),
                        LocalDate.of(2021, 9, 5),
                        5, 2.0
                    ),
                    Arbeidsgiverperiode(
                        LocalDate.of(2021, 9, 20),
                        LocalDate.of(2021, 9, 29),
                        9, 2.0
                    )
                )
            )
        }
    }

    @Test
    fun `Maks 16 dagers arbeidsgiverperiode totalt`() {
        Assertions.assertThatExceptionOfType(ConstraintViolationException::class.java).isThrownBy {
            RefusjonskravDto(
                TestData.validIdentitetsnummer,
                TestData.validOrgNr,
                setOf(
                    Arbeidsgiverperiode(
                        LocalDate.of(2021, 9, 1),
                        LocalDate.of(2021, 9, 6),
                        5, 2.0
                    ),
                    Arbeidsgiverperiode(
                        LocalDate.of(2021, 9, 10),
                        LocalDate.of(2021, 9, 20),
                        9, 2.0
                    )
                )
            )
        }
    }

    @Test
    fun `16 dagers arbeidsgiverperiode totalt er gyldig`() {
        RefusjonskravDto(
            TestData.validIdentitetsnummer,
            TestData.validOrgNr,
            setOf(
                Arbeidsgiverperiode(
                    LocalDate.of(2021, 9, 13),
                    LocalDate.of(2021, 9, 17),
                    2, 2.0
                ),
                Arbeidsgiverperiode(
                    LocalDate.of(2021, 9, 17),
                    LocalDate.of(2021, 9, 27),
                    11, 2.0
                )
            )
        )
    }

    @Test
    fun `13 dagers refusjon i samme periode er gyldig`() {
        RefusjonskravDto(
            TestData.validIdentitetsnummer,
            TestData.validOrgNr,
            setOf(
                Arbeidsgiverperiode(
                    LocalDate.of(2021, 9, 15),
                    LocalDate.of(2021, 9, 30),
                    13, 2.0
                )
            )
        )
    }

    @Test
    fun `Antall dager med refusjon kan ikke overstige periodens lengde`() {
        Assertions.assertThatExceptionOfType(ConstraintViolationException::class.java).isThrownBy {
            RefusjonskravDto(
                TestData.validIdentitetsnummer,
                TestData.validOrgNr,
                setOf(
                    Arbeidsgiverperiode(
                        LocalDate.of(2021, 9, 1),
                        LocalDate.of(2021, 9, 6),
                        7, 2.0
                    )
                )
            )
        }
    }

    @Test
    fun `Maks refusjonsdager er totalperiode minus 3 dager`() {
        Assertions.assertThatExceptionOfType(ConstraintViolationException::class.java).isThrownBy {
            RefusjonskravDto(
                TestData.validIdentitetsnummer,
                TestData.validOrgNr,
                setOf(
                    Arbeidsgiverperiode(
                        LocalDate.of(2021, 9, 1),
                        LocalDate.of(2021, 9, 6),
                        4, 2000.0
                    ),
                    Arbeidsgiverperiode(
                        LocalDate.of(2020, 4, 10),
                        LocalDate.of(2020, 4, 12),
                        3, 1500.0
                    )
                )
            )
        }
    }

    @Test
    fun `Dager uten refusjon kan være før 16 mars`() {
        RefusjonskravDto(
            TestData.validIdentitetsnummer,
            TestData.validOrgNr,
            setOf(
                Arbeidsgiverperiode(
                    LocalDate.of(2021, 9, 9),
                    LocalDate.of(2021, 9, 19),
                    4, 2000.0
                )
            )
        )
    }

    @Test
    fun `Det kan ikke kreves refusjon fra og med 1 oktober 2021 til og med 30 november 2021`() {
        Assertions.assertThatExceptionOfType(ConstraintViolationException::class.java).isThrownBy {
            RefusjonskravDto(
                TestData.validIdentitetsnummer,
                TestData.validOrgNr,
                setOf(
                    Arbeidsgiverperiode(
                        LocalDate.of(2021, 10, 1),
                        LocalDate.of(2021, 10, 8),
                        4, 2000.0
                    )
                )
            )
        }
    }

    @Test
    fun `Sammenlagte perioder må ha tre dager uten refusjonskrav`() {
        RefusjonskravDto(
            TestData.validIdentitetsnummer,
            TestData.validOrgNr,
            setOf(
                Arbeidsgiverperiode(
                    LocalDate.of(2021, 9, 10),
                    LocalDate.of(2021, 9, 15),
                    0, 0.0
                ),
                Arbeidsgiverperiode(
                    LocalDate.of(2021, 9, 20),
                    LocalDate.of(2021, 9, 25),
                    5, 10000.0
                )
            )
        )

        Assertions.assertThatExceptionOfType(ConstraintViolationException::class.java).isThrownBy {
            RefusjonskravDto(
                TestData.validIdentitetsnummer,
                TestData.validOrgNr,
                setOf(
                    Arbeidsgiverperiode(
                        LocalDate.of(2021, 9, 10),
                        LocalDate.of(2021, 9, 15),
                        5, 10000.0
                    )
                )
            )
        }
    }

    @Test
    fun `Kan ikke kreve refusjon for dager før seks måneder siden`() {
        Assertions.assertThatExceptionOfType(ConstraintViolationException::class.java).isThrownBy {
            RefusjonskravDto(
                TestData.validIdentitetsnummer,
                TestData.validOrgNr,
                setOf(
                    Arbeidsgiverperiode(
                        LocalDate.of(2021, 6, 5),
                        LocalDate.of(2021, 6, 17),
                        4, 4000.0
                    )
                )
            )
        }

        RefusjonskravDto(
            TestData.validIdentitetsnummer,
            TestData.validOrgNr,
            setOf(
                Arbeidsgiverperiode(
                    LocalDate.of(2021, 7, 10),
                    LocalDate.of(2021, 7, 17),
                    0, 0.0
                )
            )
        )
    }

    @Test
    fun `Kan ikke søke refusjon frem i tid`() {
        val today = LocalDate.now()
        Assertions.assertThatExceptionOfType(ConstraintViolationException::class.java).isThrownBy {
            RefusjonskravDto(
                TestData.validIdentitetsnummer,
                TestData.validOrgNr,
                setOf(
                    Arbeidsgiverperiode(
                        today.minusDays(6),
                        today.plusDays(3),
                        3, 1000.0
                    )
                )
            )
        }
    }

    @Test
    fun `Antall dager det kreves refusjon for kan ikke være negativt`() {
        Assertions.assertThatExceptionOfType(ConstraintViolationException::class.java).isThrownBy {
            RefusjonskravDto(
                TestData.validIdentitetsnummer,
                TestData.validOrgNr,
                setOf(
                    Arbeidsgiverperiode(
                        LocalDate.of(2020, 3, 10),
                        LocalDate.of(2020, 3, 20),
                        -5, 4000.0
                    )
                )
            )
        }
    }

    @Test
    fun `Beløp må være null hvis antall refusjonsdager er null`() {
        Assertions.assertThatExceptionOfType(ConstraintViolationException::class.java).isThrownBy {
            RefusjonskravDto(
                TestData.validIdentitetsnummer,
                TestData.validOrgNr,
                setOf(
                    Arbeidsgiverperiode(
                        LocalDate.of(2020, 3, 10),
                        LocalDate.of(2020, 3, 20),
                        0, 4000.0
                    )
                )
            )
        }
    }
}
