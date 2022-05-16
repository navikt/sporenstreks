package no.nav.helse.sporenstreks.web.dto.validation

import com.google.common.collect.Range
import com.google.common.collect.RangeSet
import com.google.common.collect.TreeRangeSet
import no.nav.helse.arbeidsgiver.integrasjoner.aareg.Arbeidsforhold
import no.nav.helse.arbeidsgiver.integrasjoner.aareg.Periode
import no.nav.helse.sporenstreks.domene.Arbeidsgiverperiode
import org.valiktor.Validator
import java.time.LocalDate

class ArbeidsforholdConstraint : CustomConstraint

const val MAKS_DAGER_OPPHOLD = 3L

fun <E> Validator<E>.Property<LocalDate?>.måHaAktivtArbeidsforhold(
    agp: Arbeidsgiverperiode,
    virksomhet: String,
    arbeidsforhold: List<Arbeidsforhold>
) = this.validate(ArbeidsforholdConstraint()) {
    val ansattPerioder = arbeidsforhold!!
        .filter { it.arbeidsgiver.organisasjonsnummer == virksomhet }
        .map { it.ansettelsesperiode.periode }
        .sortedWith(compareBy(Periode::fom, Periode::tom))

    val sammenslåttePerioder = slåSammenPerioder(ansattPerioder)

    return@validate sammenslåttePerioder.encloses(Range.open(agp.fom, agp.tom))
}

fun slåSammenPerioder(perioder: List<Periode>): RangeSet<LocalDate> {
    val sammenslåttePerioder: RangeSet<LocalDate> = TreeRangeSet.create()
    perioder.sortedWith(compareBy(Periode::fom, Periode::tom)).map { periodeToRange(it) }.forEach { r ->
        if (sammenslåttePerioder.contains(r.lowerEndpoint().minusDays(3))) {
            sammenslåttePerioder.add(Range.closed(r.lowerEndpoint().minusDays(3), r.upperEndpoint()))
        } else sammenslåttePerioder.add(r)
    }
    return sammenslåttePerioder
}
fun periodeToRange(periode: Periode): Range<LocalDate> {
    if (periode.fom!!.toEpochDay() > 0) {
        return Range.closed(periode.fom, periode.tom ?: LocalDate.MAX)
    } else return Range.closed(LocalDate.ofEpochDay(0), periode.tom ?: LocalDate.MAX)
}
