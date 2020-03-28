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
                        2, 2.3
                ), Arbeidsgiverperiode(
                        LocalDate.of(2020, 4, 22),
                        LocalDate.of(2020, 4, 29),
                        4, 2.4
                ))
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
                        2, 2.0
                ), Arbeidsgiverperiode(
                        LocalDate.of(2020, 4, 5),
                        LocalDate.of(2020, 4, 10),
                        4, 2.0
                ))
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
                            2, 1.0
                    ), Arbeidsgiverperiode(
                            LocalDate.of(2020, 4, 5),
                            LocalDate.of(2020, 4, 10),
                            4, 2.0
                    ))
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
                            2, -14.65
                    ))
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
                                    2, 2.0
                            ), Arbeidsgiverperiode(
                            LocalDate.of(2020, 4, 23),
                            LocalDate.of(2020, 4, 29),
                            8, 0.0
                    ))
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
                                    5, 2.0
                            ), Arbeidsgiverperiode(
                            LocalDate.of(2020, 4, 20),
                            LocalDate.of(2020, 4, 29),
                            9, 2.0
                    ))
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
                                    5, 2.0
                            ), Arbeidsgiverperiode(
                            LocalDate.of(2020, 4, 10),
                            LocalDate.of(2020, 4, 20),
                            9, 2.0
                    ))
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
                                2, 2.0
                        ), Arbeidsgiverperiode(
                        LocalDate.of(2020, 4, 10),
                        LocalDate.of(2020, 4, 19),
                        10, 2.0
                ))
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
                                    7, 2.0))
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
                                    4, 2000.0
                            ), Arbeidsgiverperiode(
                            LocalDate.of(2020, 4, 10),
                            LocalDate.of(2020, 4, 12),
                            3, 1500.0
                    ))
            )
        }
    }

    @Test
    fun `Får ikke refusjon over 6G`() {
        Assertions.assertThatExceptionOfType(ConstraintViolationException::class.java).isThrownBy {
            RefusjonskravDto(
                    TestData.validIdentitetsnummer,
                    TestData.validOrgNr,
                    setOf(
                            Arbeidsgiverperiode(
                                    LocalDate.of(2020, 4, 1),
                                    LocalDate.of(2020, 4, 6),
                                    3, 6000.0
                            ), Arbeidsgiverperiode(
                            LocalDate.of(2020, 4, 10),
                            LocalDate.of(2020, 4, 15),
                            3, 10000.0
                    ))
            )
        }
    }

    @Test
    fun `Dager uten refusjon kan være før 16 mars`() {
        RefusjonskravDto(
                TestData.validIdentitetsnummer,
                TestData.validOrgNr,
                setOf(Arbeidsgiverperiode(
                        LocalDate.of(2020, 3, 9),
                        LocalDate.of(2020, 3, 19),
                        4, 2000.0
                ))
        )
    }

    @Test
    fun `Sammenlagte perioder må ha tre dager uten refusjonskrav`() {
        RefusjonskravDto(
                TestData.validIdentitetsnummer,
                TestData.validOrgNr,
                setOf(
                        Arbeidsgiverperiode(
                                LocalDate.of(2020, 3, 10),
                                LocalDate.of(2020, 3, 15),
                                0, 0.0
                        ), Arbeidsgiverperiode(
                        LocalDate.of(2020, 3, 20),
                        LocalDate.of(2020, 3, 25),
                        5, 10000.0
                ))
        )

        Assertions.assertThatExceptionOfType(ConstraintViolationException::class.java).isThrownBy {
            RefusjonskravDto(
                    TestData.validIdentitetsnummer,
                    TestData.validOrgNr,
                    setOf(Arbeidsgiverperiode(
                            LocalDate.of(2020, 4, 10),
                            LocalDate.of(2020, 4, 15),
                            5, 10000.0
                    ))
            )
        }
    }

    @Test
    fun `Må inneholde minst en periode med dager etter 16 mars`() {
        Assertions.assertThatExceptionOfType(ConstraintViolationException::class.java).isThrownBy {
            RefusjonskravDto(
                    TestData.validIdentitetsnummer,
                    TestData.validOrgNr,
                    setOf(Arbeidsgiverperiode(
                            LocalDate.of(2020, 2, 20),
                            LocalDate.of(2020, 2, 25),
                            0, 0.0
                    ),
                            Arbeidsgiverperiode(
                                    LocalDate.of(2020, 3, 10),
                                    LocalDate.of(2020, 3, 15),
                                    5, 10000.0
                            ))
            )
        }
    }


    @Test
    fun `Kan ikke kreve refusjon for dager før 16 mars`() {
        Assertions.assertThatExceptionOfType(ConstraintViolationException::class.java).isThrownBy {
            RefusjonskravDto(
                    TestData.validIdentitetsnummer,
                    TestData.validOrgNr,
                    setOf(
                            Arbeidsgiverperiode(
                                    LocalDate.of(2020, 3, 10),
                                    LocalDate.of(2020, 3, 17),
                                    4, 4000.0))
            )
        }

        RefusjonskravDto(
                TestData.validIdentitetsnummer,
                TestData.validOrgNr,
                setOf(
                        Arbeidsgiverperiode(
                                LocalDate.of(2020, 3, 10),
                                LocalDate.of(2020, 3, 17),
                                0, 0.0))
        )
    }

}