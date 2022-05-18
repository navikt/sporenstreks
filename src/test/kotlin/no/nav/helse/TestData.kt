package no.nav.helse

import no.nav.helse.arbeidsgiver.integrasjoner.aareg.*
import no.nav.helse.sporenstreks.domene.Arbeidsgiverperiode
import no.nav.helse.sporenstreks.domene.Refusjonskrav
import no.nav.helse.sporenstreks.domene.RefusjonskravStatus
import java.time.LocalDate
import java.time.LocalDate.of
import java.time.LocalDateTime

object TestData {
    val validIdentitetsnummer = "20015001543"
    val notValidIdentitetsnummer = "50012001987"
    val virksomhetsNavn = "Test virksomhet"
    val validOrgNr = "123456785"
    val notValidOrgNr = "123456789"
    val opprettetAv = "20015001543"
    val gyldigKrav = Refusjonskrav(
        opprettetAv,
        validIdentitetsnummer,
        validOrgNr,
        setOf(Arbeidsgiverperiode(of(2022, 4, 4), of(2022, 4, 10), 2, 1000.0)),
        RefusjonskravStatus.MOTTATT
    )

    val arbeidsgiver = Arbeidsgiver("AS", validOrgNr)
    val opplysningspliktig = Opplysningspliktig("AS", "1212121212")

    val arbeidsForhold = listOf(
        Arbeidsforhold(
            Arbeidsgiver("AS", "123456785"),
            Opplysningspliktig("AS", "1212121212"),
            emptyList(),
            Ansettelsesperiode(
                Periode(
                    LocalDate.of(2021, 1, 1),
                    null
                )
            ),
            LocalDateTime.now()
        )
    )

    val flereArbeidsForholdISammeOrg = listOf(
        Arbeidsforhold(
            Arbeidsgiver("AS", "123456785"),
            Opplysningspliktig("AS", "1212121212"),
            emptyList(),
            Ansettelsesperiode(
                Periode(
                    LocalDate.of(2022, 1, 1),
                    LocalDate.of(2022, 1, 31)
                )
            ),
            LocalDateTime.now()
        ),
        Arbeidsforhold(
            Arbeidsgiver("AS", "123456785"),
            Opplysningspliktig("AS", "1212121212"),
            emptyList(),
            Ansettelsesperiode(
                Periode(
                    LocalDate.of(2021, 5, 20),
                    LocalDate.of(2021, 12, 31)
                )
            ),
            LocalDateTime.now()
        )
    )

    val evigArbeidsForholdListe = listOf(
        Arbeidsforhold(
            arbeidsgiver,
            opplysningspliktig,
            emptyList(),
            Ansettelsesperiode(
                Periode(
                    LocalDate.MIN,
                    LocalDate.MAX
                )
            ),
            LocalDateTime.now()
        )
    )
    val avsluttetArbeidsforholdListe = listOf(
        Arbeidsforhold(
            arbeidsgiver,
            opplysningspliktig,
            emptyList(),
            Ansettelsesperiode(
                Periode(
                    LocalDate.MIN,
                    LocalDate.of(2021, 2, 5)
                )
            ),
            LocalDateTime.now()
        )
    )

    val pågåendeArbeidsforholdListe = listOf(
        Arbeidsforhold(
            arbeidsgiver,
            opplysningspliktig,
            emptyList(),
            Ansettelsesperiode(
                Periode(
                    LocalDate.of(2021, 2, 5),
                    null
                )
            ),
            LocalDateTime.now()
        )
    )
    val arbeidsforholdMedSluttDato = listOf(
        Arbeidsforhold(
            arbeidsgiver,
            opplysningspliktig,
            emptyList(),
            Ansettelsesperiode(
                Periode(
                    LocalDate.of(2004, 6, 1),
                    LocalDate.of(2004, 6, 30),
                )
            ),
            LocalDateTime.now()
        ),
        Arbeidsforhold(
            arbeidsgiver,
            opplysningspliktig,
            emptyList(),
            Ansettelsesperiode(
                Periode(
                    LocalDate.of(2004, 9, 1),
                    LocalDate.of(2004, 9, 30)
                )
            ),
            LocalDateTime.now()
        ),
        Arbeidsforhold(
            arbeidsgiver,
            opplysningspliktig,
            emptyList(),
            Ansettelsesperiode(
                Periode(
                    LocalDate.of(2005, 1, 1),
                    LocalDate.of(2005, 2, 28)
                )
            ),
            LocalDateTime.now()
        ),
        Arbeidsforhold(
            arbeidsgiver,
            opplysningspliktig,
            emptyList(),
            Ansettelsesperiode(
                Periode(
                    LocalDate.of(2005, 9, 6),
                    LocalDate.of(2007, 12, 31)
                )
            ),
            LocalDateTime.now()
        ),
        Arbeidsforhold(
            arbeidsgiver,
            opplysningspliktig,
            emptyList(),
            Ansettelsesperiode(
                Periode(
                    LocalDate.of(2008, 6, 16),
                    LocalDate.of(2008, 8, 3)
                )
            ),
            LocalDateTime.now()
        ),
        Arbeidsforhold(
            arbeidsgiver,
            opplysningspliktig,
            emptyList(),
            Ansettelsesperiode(
                Periode(
                    LocalDate.of(2009, 3, 5),
                    LocalDate.of(2010, 8, 30)
                )
            ),
            LocalDateTime.now()
        ),
        Arbeidsforhold(
            arbeidsgiver,
            opplysningspliktig,
            emptyList(),
            Ansettelsesperiode(
                Periode(
                    LocalDate.of(2010, 11, 26),
                    LocalDate.of(2011, 9, 4)
                )
            ),
            LocalDateTime.now()
        ),
        Arbeidsforhold(
            arbeidsgiver,
            opplysningspliktig,
            emptyList(),
            Ansettelsesperiode(
                Periode(
                    LocalDate.of(2011, 9, 5),
                    LocalDate.of(2013, 3, 30)
                )
            ),
            LocalDateTime.now()
        ),
        Arbeidsforhold(
            arbeidsgiver,
            opplysningspliktig,
            emptyList(),
            Ansettelsesperiode(
                Periode(
                    LocalDate.of(2013, 3, 31),
                    LocalDate.of(2014, 1, 1)
                )
            ),
            LocalDateTime.now()
        ),
        Arbeidsforhold(
            arbeidsgiver,
            opplysningspliktig,
            emptyList(),
            Ansettelsesperiode(
                Periode(
                    LocalDate.of(2013, 3, 31),
                    LocalDate.of(2013, 3, 31)
                )
            ),
            LocalDateTime.now()
        ),
        Arbeidsforhold(
            arbeidsgiver,
            opplysningspliktig,
            emptyList(),
            Ansettelsesperiode(
                Periode(
                    LocalDate.of(2014, 2, 24),
                    LocalDate.of(2014, 2, 24)
                )
            ),
            LocalDateTime.now()
        ),
        Arbeidsforhold(
            arbeidsgiver,
            opplysningspliktig,
            emptyList(),
            Ansettelsesperiode(
                Periode(
                    LocalDate.of(2014, 3, 28),
                    LocalDate.of(2014, 5, 31)
                )
            ),
            LocalDateTime.now()
        ),
        Arbeidsforhold(
            arbeidsgiver,
            opplysningspliktig,
            emptyList(),
            Ansettelsesperiode(
                Periode(
                    LocalDate.of(2014, 6, 1),
                    LocalDate.of(2022, 4, 30)
                )
            ),
            LocalDateTime.now()
        ),
        Arbeidsforhold(
            arbeidsgiver,
            opplysningspliktig,
            emptyList(),
            Ansettelsesperiode(
                Periode(
                    LocalDate.of(2014, 6, 1),
                    LocalDate.of(2014, 12, 31)
                )
            ),
            LocalDateTime.now()
        ),
        Arbeidsforhold(
            arbeidsgiver,
            opplysningspliktig,
            emptyList(),
            Ansettelsesperiode(
                Periode(
                    LocalDate.of(2022, 5, 1),
                    null
                )
            ),
            LocalDateTime.now()
        )
    )

    val listeMedEttArbeidsforhold = listOf(
        Arbeidsforhold(
            arbeidsgiver,
            opplysningspliktig,
            emptyList(),
            Ansettelsesperiode(
                Periode(
                    LocalDate.of(2019, 1, 1),
                    LocalDate.of(2021, 2, 28)
                )
            ),
            LocalDateTime.now()
        ),

        Arbeidsforhold(
            arbeidsgiver,
            opplysningspliktig,
            emptyList(),
            Ansettelsesperiode(
                Periode(
                    LocalDate.of(2021, 3, 1),
                    null
                )
            ),
            LocalDateTime.now()
        )
    )
}
