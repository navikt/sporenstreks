package no.nav.helse.sporenstreks.web.dto.validation

import no.nav.helse.sporenstreks.domene.Arbeidsgiverperiode
import java.time.LocalDate

fun innenforGammelPeriode(arbeidsgiverPeriode: Arbeidsgiverperiode): Boolean {
    val seksMndSiden = LocalDate.now().minusMonths(6)
    return arbeidsgiverPeriode.fom.isAfter(seksMndSiden) &&
        arbeidsgiverPeriode.tom.isBefore(Arbeidsgiverperiode.refusjonTilDatoGammelPeriode)
}

fun innenforNyPeriode(arbeidsgiverPeriode: Arbeidsgiverperiode): Boolean {
    return arbeidsgiverPeriode.fom.isAfter(Arbeidsgiverperiode.refusjonFraDatoNyPeriode) &&
        arbeidsgiverPeriode.tom.isBefore(Arbeidsgiverperiode.refusjonTilDatoNyPeriode)
}

fun antallDagerArbeidsgiverBetalerFor(arbeidsgiverperiode: Arbeidsgiverperiode): Int {
    return if (innenforGammelPeriode(arbeidsgiverperiode))
        Arbeidsgiverperiode.arbeidsgiverBetalerForDagerGammelPeriode
    else Arbeidsgiverperiode.arbeidsgiverBetalerForDagerNyPeriode
}
