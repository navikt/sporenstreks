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
) = this.validate(ArbeidsforholdConstraint()) {
    val ansattPerioder = arbeidsforhold!!
        .filter { it.arbeidsgiver.organisasjonsnummer == refusjonskrav.virksomhetsnummer }
        .map { it.ansettelsesperiode.periode }

    val sammenhengendePerioder = slåSammenPerioder(ansattPerioder)

    return@validate refusjonskrav.perioder.all { p ->
        sammenhengendePerioder.encloses(Range.closed(p.fom, p.tom))
    }
}
fun slåSammenPerioder(arbeidsforholdPerioder: List<Periode>): RangeSet<LocalDate> {
    val sammenhengendePerioder: RangeSet<LocalDate> = TreeRangeSet.create()
    arbeidsforholdPerioder.sortedWith(compareBy(Periode::fom, Periode::tom)).forEach { ansattPeriode ->
        val f = if (sammenhengendePerioder.intersects(Range.atLeast(ansattPeriode.fom!!.minusDays(3)))) {
            ansattPeriode.fom!!.minusDays(3)
        } else ansattPeriode.fom
        if (ansattPeriode.tom == null) sammenhengendePerioder.add(Range.atLeast(f))
        else sammenhengendePerioder.add(Range.closed(f, ansattPeriode.tom))
    }
    return sammenhengendePerioder
}
