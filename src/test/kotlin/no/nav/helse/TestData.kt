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
}
