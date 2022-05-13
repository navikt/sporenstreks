package no.nav.helse.sporenstreks.web.dto.validation

import no.nav.helse.arbeidsgiver.integrasjoner.aareg.Arbeidsforhold
import no.nav.helse.arbeidsgiver.integrasjoner.aareg.Periode
import no.nav.helse.sporenstreks.domene.Arbeidsgiverperiode
import no.nav.helse.sporenstreks.web.dto.RefusjonskravDto
import org.valiktor.Validator

class ArbeidsforholdConstraint : CustomConstraint

const val MAKS_DAGER_OPPHOLD = 3L

fun <E> Validator<E>.Property<Iterable<Arbeidsgiverperiode>?>.måHaAktivtArbeidsforhold(
    refusjonskrav: RefusjonskravDto,
    arbeidsforhold: List<Arbeidsforhold>?
) = this.validate(ArbeidsforholdConstraint()) {
    val ansattPerioder = arbeidsforhold!!
        .filter { it.arbeidsgiver.organisasjonsnummer == refusjonskrav.virksomhetsnummer }
        .map { it.ansettelsesperiode.periode }

    val sammenhengedePerioder = slåSammenPerioder(ansattPerioder)

    refusjonskrav.perioder.all { kravPeriode ->
        sammenhengedePerioder.any { ansattPeriode ->
            (ansattPeriode.tom == null || kravPeriode.tom.isBefore(ansattPeriode.tom) || kravPeriode.tom == ansattPeriode.tom) &&
                ansattPeriode.fom!!.isBefore(kravPeriode.fom)
        }
    }
}

fun slåSammenPerioder(arbeidsforholdPerioder: List<Periode>): List<Periode> {
    if (arbeidsforholdPerioder.size < 2) return arbeidsforholdPerioder

    val perioder = arbeidsforholdPerioder
        .sortedWith(compareBy(Periode::fom, Periode::tom))
        .toMutableList()

    // Legg til første periode
    val sammenhengedePerioder = mutableListOf(perioder.removeFirst())

    perioder.forEach { gjeldendePeriode ->
        val forrigePeriode = sammenhengedePerioder.last()
        // Hvis periode gjeldendePeriode og forrigePeriode overlapper
        if (overlapperPeriode(gjeldendePeriode, forrigePeriode)) {
            // Hvis gjeldendePeriode.tom er null eller før forrigePeriode.tom oppdater forrigePeriode
            if (gjeldendePeriode.tom == null || gjeldendePeriode.tom!! > forrigePeriode.tom) {
                sammenhengedePerioder[sammenhengedePerioder.lastIndex] = Periode(forrigePeriode.fom, gjeldendePeriode.tom)
            }
            return@forEach
        }

        sammenhengedePerioder.add(gjeldendePeriode)
    }

    return sammenhengedePerioder
}

fun overlapperPeriode(
    gjeldendePeriode: Periode,
    forrigePeriode: Periode
): Boolean {
    if (forrigePeriode.tom == null) return true
    val gjeldendePeriodeFom = gjeldendePeriode.fom!!.minusDays(MAKS_DAGER_OPPHOLD)
    // Hvis gjeldende periode (fra og med) er før eller lik som forrige periode (til og med)
    return gjeldendePeriodeFom <= forrigePeriode.tom
}
