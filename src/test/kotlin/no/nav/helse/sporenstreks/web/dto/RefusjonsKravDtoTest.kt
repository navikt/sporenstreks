package no.nav.helse.sporenstreks.web.dto

import no.nav.helse.TestData
import no.nav.helse.sporenstreks.domene.Arbeidsgiverperiode
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.valiktor.ConstraintViolationException
import java.time.LocalDate

internal class RefusjonsKravDtoTest {

    @Test
    fun `Gyldig refusjonskrav validerer OK`() {
        RefusjonskravDto(
                TestData.validIdentitetsnummer,
                TestData.validOrgNr,
                setOf(Arbeidsgiverperiode(
                        LocalDate.of(2020, 4, 1),
                        LocalDate.of(2020, 4, 5),
                        2
                ), Arbeidsgiverperiode(
                        LocalDate.of(2020, 4, 22),
                        LocalDate.of(2020, 4, 29),
                        4
                )),
                3272.3
        )
    }

    @Test
    fun `Kant-i-kant perioder er lov`() {
        RefusjonskravDto(
                TestData.validIdentitetsnummer,
                TestData.validOrgNr,
                setOf(Arbeidsgiverperiode(
                        LocalDate.of(2020, 4, 1),
                        LocalDate.of(2020, 4, 5),
                        2
                ), Arbeidsgiverperiode(
                        LocalDate.of(2020, 4, 5),
                        LocalDate.of(2020, 4, 10),
                        4
                )),
                3272.3
        )
    }

    @Test
    fun `Overlappende perioder er ikke lov`() {
        Assertions.assertThatExceptionOfType(ConstraintViolationException::class.java).isThrownBy {
            RefusjonskravDto(
                    TestData.validIdentitetsnummer,
                    TestData.validOrgNr,
                    setOf(Arbeidsgiverperiode(
                            LocalDate.of(2020, 4, 1),
                            LocalDate.of(2020, 4, 6),
                            2
                    ), Arbeidsgiverperiode(
                            LocalDate.of(2020, 4, 5),
                            LocalDate.of(2020, 4, 10),
                            4
                    )),
                    3272.3
            )
        }
    }

    @Test
    fun `Negativt beløp feiler`() {
        Assertions.assertThatExceptionOfType(ConstraintViolationException::class.java).isThrownBy {
            RefusjonskravDto(
                    TestData.validIdentitetsnummer,
                    TestData.validOrgNr,
                    setOf(Arbeidsgiverperiode(
                            LocalDate.of(2020, 4, 1),
                            LocalDate.of(2020, 4, 5),
                            2
                    )),
                    -1.0
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
                                    LocalDate.of(2020, 4, 1),
                                    LocalDate.of(2020, 4, 5),
                                    2
                            ), Arbeidsgiverperiode(
                            LocalDate.of(2020, 4, 23),
                            LocalDate.of(2020, 4, 29),
                            8
                    )),
                    123.8
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
                                    LocalDate.of(2020, 4, 1),
                                    LocalDate.of(2020, 4, 5),
                                    5
                            ), Arbeidsgiverperiode(
                            LocalDate.of(2020, 4, 20),
                            LocalDate.of(2020, 4, 29),
                            9
                    )),
                    123.8
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
                                    LocalDate.of(2020, 4, 1),
                                    LocalDate.of(2020, 4, 6),
                                    5
                            ), Arbeidsgiverperiode(
                            LocalDate.of(2020, 4, 10),
                            LocalDate.of(2020, 4, 20),
                            9
                    )),
                    123.8
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
                                    LocalDate.of(2020, 4, 1),
                                    LocalDate.of(2020, 4, 6),
                                    2
                            ), Arbeidsgiverperiode(
                            LocalDate.of(2020, 4, 10),
                            LocalDate.of(2020, 4, 19),
                            10
                    )),
                    123.8
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
                                    LocalDate.of(2020, 4, 1),
                                    LocalDate.of(2020, 4, 6),
                                    7)),
                    123.8
            )
        }
    }

    @Test
    fun `Periode før 16 mars er ikke gyldige`() {
        Assertions.assertThatExceptionOfType(ConstraintViolationException::class.java).isThrownBy {
            RefusjonskravDto(
                    TestData.validIdentitetsnummer,
                    TestData.validOrgNr,
                    setOf(
                            Arbeidsgiverperiode(
                                    LocalDate.of(2020, 3, 15),
                                    LocalDate.of(2020, 3, 20),
                                    2)),
                    123.8
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
                                    LocalDate.of(2020, 4, 1),
                                    LocalDate.of(2020, 4, 6),
                                    4
                            ), Arbeidsgiverperiode(
                            LocalDate.of(2020, 4, 10),
                            LocalDate.of(2020, 4, 12),
                            3
                    )),
                    123.8
            )
        }
    }
}