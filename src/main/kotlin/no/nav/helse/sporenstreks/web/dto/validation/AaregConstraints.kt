package no.nav.helse.sporenstreks.web.dto.validation

import no.nav.helse.arbeidsgiver.integrasjoner.aareg.Arbeidsforhold
import no.nav.helse.arbeidsgiver.integrasjoner.aareg.Periode
import no.nav.helse.sporenstreks.domene.Arbeidsgiverperiode
import no.nav.helse.sporenstreks.web.dto.RefusjonskravDto
import org.valiktor.Validator

class ArbeidsforholdConstraint : CustomConstraint

fun <E> Validator<E>.Property<Iterable<Arbeidsgiverperiode>?>.måHaAktivtArbeidsforhold(
    refusjonskrav: RefusjonskravDto,
    arbeidsforhold: List<Arbeidsforhold>?
) = this.validate(ArbeidsforholdConstraint()) {
    val aktuelleArbeidsforhold = arbeidsforhold!!
        .filter { it.arbeidsgiver.organisasjonsnummer == refusjonskrav.virksomhetsnummer }
        .map { it.ansettelsesperiode.periode }

    val sammenslåtteAnsattPerioder = slåSammenPerioder(aktuelleArbeidsforhold)

    refusjonskrav.perioder.all { kravPeriode ->
        sammenslåtteAnsattPerioder.any { ansattPeriode ->
            kravInnenforArbeidsgiverperiode(ansattPeriode, kravPeriode)
        } || aktuelleArbeidsforhold.any { ansattPeriode ->
            kravInnenforArbeidsgiverperiode(ansattPeriode, kravPeriode)
        }
    }
}

fun slåSammenPerioder(list: List<Periode>): List<Periode> {
    if (list.size < 2) return list

    val periods = list
        .sortedWith(compareBy(Periode::fom, Periode::tom))

    val merged = mutableListOf<Periode>()

    periods.forEach { gjeldendePeriode ->
        // Legg til første periode
        if (merged.size == 0) {
            merged.add(gjeldendePeriode)
            return@forEach
        }

        val forrigePeriode = merged.last()
        // Hvis periode overlapper, oppdater tom
        if (overlapperPeriode(gjeldendePeriode, forrigePeriode)) {
            merged[merged.lastIndex] = Periode(forrigePeriode.fom, gjeldendePeriode.tom)
            return@forEach
        }

        merged.add(gjeldendePeriode)
    }

    return merged
}

fun kravInnenforArbeidsgiverperiode(ansattPeriode: Periode, kravPeriode: Arbeidsgiverperiode): Boolean {
    return (ansattPeriode.tom == null || kravPeriode.tom.isBefore(ansattPeriode.tom) || kravPeriode.tom == ansattPeriode.tom) &&
        ansattPeriode.fom!!.isBefore(kravPeriode.fom)
}

fun overlapperPeriode(
    gjeldendePeriode: Periode,
    forrigePeriode: Periode,
    dager: Long = 3L // MAKS_DAGER_OPPHOLD
): Boolean {
    return (
        gjeldendePeriode.fom!!.isBefore(forrigePeriode.tom?.plusDays(dager)) ||
            gjeldendePeriode.fom!!.isEqual(forrigePeriode.tom?.plusDays(dager))
        )
}
