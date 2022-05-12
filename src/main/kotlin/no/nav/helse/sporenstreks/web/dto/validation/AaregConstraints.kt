package no.nav.helse.sporenstreks.web.dto.validation

import com.google.common.collect.Range
import com.google.common.collect.RangeSet
import com.google.common.collect.TreeRangeSet
import no.nav.helse.arbeidsgiver.integrasjoner.aareg.Arbeidsforhold
import no.nav.helse.arbeidsgiver.integrasjoner.aareg.Periode
import no.nav.helse.sporenstreks.domene.Arbeidsgiverperiode
import no.nav.helse.sporenstreks.web.dto.RefusjonskravDto
import org.valiktor.Validator
import java.time.LocalDate

class ArbeidsforholdConstraint : CustomConstraint

const val MAKS_DAGER_OPPHOLD = 3L

fun <E> Validator<E>.Property<Iterable<Arbeidsgiverperiode>?>.måHaAktivtArbeidsforhold(
    refusjonskrav: RefusjonskravDto,
    arbeidsforhold: List<Arbeidsforhold>?
) =
    måHaAktivtArbeidsforholdEnkel(refusjonskrav.perioder, refusjonskrav.virksomhetsnummer, arbeidsforhold)

fun <E> Validator<E>.Property<Iterable<Arbeidsgiverperiode>?>.måHaAktivtArbeidsforholdEnkel(
    arbeidsgiverPerioder: Set<Arbeidsgiverperiode>,
    virksomhet: String,
    arbeidsforhold: List<Arbeidsforhold>?
) = this.validate(ArbeidsforholdConstraint()) {
    val ansattPerioder = arbeidsforhold!!
        .filter { it.arbeidsgiver.organisasjonsnummer == virksomhet }
        .map { it.ansettelsesperiode.periode }
        .sortedWith(compareBy(Periode::fom, Periode::tom))

    val sammenslåttePerioder = slåSammenPerioder(ansattPerioder)

    arbeidsgiverPerioder
        .all { periode -> sammenslåttePerioder.encloses(Range.open(periode.fom, periode.tom)) }
}

fun slåSammenPerioder(perioder: List<Periode>): RangeSet<LocalDate> {
    val sammenslåttePerioder: RangeSet<LocalDate> = TreeRangeSet.create()
    perioder.sortedWith(compareBy(Periode::fom, Periode::tom)).forEach { periode ->
        val fomTidligst = if (periode.fom!!.year > 0) periode.fom!!.minusDays(MAKS_DAGER_OPPHOLD) else LocalDate.of(0, 1, 1)
        val overlappendePeriode = sammenslåttePerioder.intersects(Range.atLeast(fomTidligst))
        val f = if (overlappendePeriode) { fomTidligst } else periode.fom
        if (periode.tom == null) sammenslåttePerioder.add(Range.atLeast(f))
        else sammenslåttePerioder.add(Range.closed(f, periode.tom))
    }
    return sammenslåttePerioder
}