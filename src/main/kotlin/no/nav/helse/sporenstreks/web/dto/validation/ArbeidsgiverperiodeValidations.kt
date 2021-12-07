package no.nav.helse.sporenstreks.web.dto.validation

import no.nav.helse.sporenstreks.domene.Arbeidsgiverperiode
import java.time.LocalDate

fun innenforGammelPeriode(arbeidsgiverPeriode: Arbeidsgiverperiode): Boolean {
    val treMndSiden = LocalDate.now().minusMonths(3).minusDays(1)
    return arbeidsgiverPeriode.fom.isAfter(treMndSiden) &&
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
