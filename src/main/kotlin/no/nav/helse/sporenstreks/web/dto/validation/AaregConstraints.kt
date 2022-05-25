package no.nav.helse.sporenstreks.web.dto.validation

import no.nav.helse.arbeidsgiver.integrasjoner.aareg.Arbeidsforhold
import no.nav.helse.sporenstreks.domene.Arbeidsgiverperiode
import no.nav.helse.sporenstreks.utils.isBeforeOrEqual
import no.nav.helse.sporenstreks.web.dto.RefusjonskravDto
import org.valiktor.Validator
import java.time.LocalDate
import java.time.LocalDate.MAX

class ArbeidsforholdConstraint : CustomConstraint

const val MAKS_DAGER_OPPHOLD = 3L

data class AaregPeriode(
    var fom: LocalDate,
    var tom: LocalDate
) {
    fun overlapperPeriode(
        periode: AaregPeriode,
        dager: Long = MAKS_DAGER_OPPHOLD
    ): Boolean = this.fom.minusDays(dager).isBeforeOrEqual(periode.tom)
}

fun <E> Validator<E>.Property<Iterable<Arbeidsgiverperiode>?>.måHaAktivtArbeidsforhold(
    refusjonskrav: RefusjonskravDto,
    arbeidsforhold: List<Arbeidsforhold>
) = this.validate(ArbeidsforholdConstraint()) {
    val ansattPerioder = arbeidsforhold!!
        .asSequence()
        .filter { it.arbeidsgiver.organisasjonsnummer == refusjonskrav.virksomhetsnummer }
        .map { it.ansettelsesperiode.periode }
        .map { AaregPeriode(it.fom!!, it.tom ?: MAX) }
        .sortedBy { it.fom }
        .toMutableList()

    val sammenslåttePerioder = slåSammenPerioder(ansattPerioder)

    return@validate refusjonskrav.perioder.all { agp ->
        sammenslåttePerioder.any { ansattPeriode ->
            (agp.tom.isBeforeOrEqual(ansattPeriode.tom)) && (ansattPeriode.fom.isBeforeOrEqual(agp.fom))
        }
    }
}
fun <E> Validator<E>.Property<LocalDate?>.måHaAktivtArbeidsforholdEnkel(
    agp: Arbeidsgiverperiode,
    virksomhet: String,
    arbeidsforhold: List<Arbeidsforhold>
) = this.validate(ArbeidsforholdConstraint()) {
    val ansattPerioder = arbeidsforhold!!
        .asSequence()
        .filter { it.arbeidsgiver.organisasjonsnummer == virksomhet }
        .map { it.ansettelsesperiode.periode }
        .map { AaregPeriode(it.fom!!, it.tom ?: MAX) }
        .sortedBy { it.fom }
        .toMutableList()

    val sammenslåttePerioder = slåSammenPerioder(ansattPerioder)

    return@validate sammenslåttePerioder.any { ansattPeriode ->
        (agp.tom.isBeforeOrEqual(ansattPeriode.tom)) && (ansattPeriode.fom.isBeforeOrEqual(agp.fom))
    }
}

fun slåSammenPerioder(perioder: MutableList<AaregPeriode>): List<AaregPeriode> {
    if (perioder.size < 2) return perioder

    val sammenslåttePerioder = mutableListOf(perioder.removeFirst())

    perioder.forEach { gjeldendePeriode ->
        sammenslåttePerioder.find {
            gjeldendePeriode.overlapperPeriode(it)
        }?.let { overlappendePeriode ->
            with(overlappendePeriode) {
                fom = minOf(fom, gjeldendePeriode.fom)
                tom = maxOf(tom, gjeldendePeriode.tom)
            }
            return@forEach
        }
        sammenslåttePerioder.add(gjeldendePeriode)
    }

    return sammenslåttePerioder
}
