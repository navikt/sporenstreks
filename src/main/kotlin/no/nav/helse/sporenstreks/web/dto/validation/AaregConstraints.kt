package no.nav.helse.sporenstreks.web.dto.validation

import no.nav.helse.arbeidsgiver.integrasjoner.aareg.Arbeidsforhold
import no.nav.helse.arbeidsgiver.integrasjoner.aareg.Periode
import no.nav.helse.sporenstreks.domene.Arbeidsgiverperiode
import no.nav.helse.sporenstreks.web.dto.RefusjonskravDto
import org.valiktor.Validator

class ArbeidsforholdConstraint : CustomConstraint

val MAKS_DAGER_OPPHOLD = 3L

fun <E> Validator<E>.Property<Iterable<Arbeidsgiverperiode>?>.måHaAktivtArbeidsforhold(
    refusjonskrav: RefusjonskravDto,
    arbeidsforhold: List<Arbeidsforhold>?
) = this.validate(ArbeidsforholdConstraint()) {
    val ansattPerioder = arbeidsforhold!!
        .filter { it.arbeidsgiver.organisasjonsnummer == refusjonskrav.virksomhetsnummer }
        .map { it.ansettelsesperiode.periode }

    val sammenhengedePerioder = slåSammenPerioder(ansattPerioder)

    return@validate refusjonskrav.perioder.all { kravPeriode ->
        sammenhengedePerioder.any { ansattPeriode ->
            (ansattPeriode.tom == null || kravPeriode.tom.isBefore(ansattPeriode.tom) || kravPeriode.tom == ansattPeriode.tom) &&
                ansattPeriode.fom!!.isBefore(kravPeriode.fom)
        } || ansattPerioder.any { ansattPeriode ->
            (ansattPeriode.tom == null || kravPeriode.tom.isBefore(ansattPeriode.tom) || kravPeriode.tom == ansattPeriode.tom) &&
                ansattPeriode.fom!!.isBefore(kravPeriode.fom)
        }
    }
}

fun slåSammenPerioder(list: List<Periode>): List<Periode> {
    if (list.size < 2) return list

    val remainingPeriods = list
        .sortedBy { it.fom }
        .toMutableList()

    val merged = ArrayList<Periode>()

    do {
        var currentPeriod = remainingPeriods[0]
        remainingPeriods.removeAt(0)

        do {
            val connectedPeriod = remainingPeriods
                .find { !oppholdMellomPerioderOverstigerDager(currentPeriod, it, MAKS_DAGER_OPPHOLD) }
            if (connectedPeriod != null) {
                currentPeriod = Periode(currentPeriod.fom, connectedPeriod.tom)
                remainingPeriods.remove(connectedPeriod)
            }
        } while (connectedPeriod != null)

        merged.add(currentPeriod)
    } while (remainingPeriods.isNotEmpty())

    return merged
}

fun oppholdMellomPerioderOverstigerDager(
    a1: Periode,
    a2: Periode,
    dager: Long
): Boolean {
    return a1.tom?.plusDays(dager)?.isBefore(a2.fom) ?: true
}
