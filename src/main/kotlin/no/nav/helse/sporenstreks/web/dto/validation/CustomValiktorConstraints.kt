package no.nav.helse.sporenstreks.web.dto.validation

import no.nav.helse.sporenstreks.domene.Arbeidsgiverperiode
import org.valiktor.Constraint
import org.valiktor.Validator
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.min

interface CustomConstraint : Constraint {
    override val messageBundle: String
        get() = "validation/validation-messages"
}

class IdentitetsnummerConstraint : CustomConstraint

fun <E> Validator<E>.Property<String?>.isValidIdentitetsnummer() =
    this.validate(IdentitetsnummerConstraint()) { FoedselsNrValidator.isValid(it) }

class OrganisasjonsnummerConstraint : CustomConstraint

fun <E> Validator<E>.Property<String?>.isValidOrganisasjonsnummer() =
    this.validate(OrganisasjonsnummerConstraint()) { OrganisasjonsnummerValidator.isValid(it) }

class RefusjonsDagerConstraint : CustomConstraint

fun <E> Validator<E>.Property<Iterable<Arbeidsgiverperiode>?>.arbeidsgiverBetalerForDager(d: LocalDate) =
    this.validate(RefusjonsDagerConstraint()) { ps ->
        var refusjonsdager = 0
        var arbeidsgiverdagerUtenRefusjon = 0
        ps!!.forEach() {
            if (it.fom >= d) {

                refusjonsdager += ChronoUnit.DAYS.between(it.fom, it.tom.plusDays(1)).toInt()
            } else if (it.tom < d) {
                arbeidsgiverdagerUtenRefusjon += ChronoUnit.DAYS.between(it.fom, it.tom.plusDays(1)).toInt()
            } else {
                refusjonsdager += ChronoUnit.DAYS.between(d, it.tom.plusDays(1)).toInt()
                arbeidsgiverdagerUtenRefusjon += ChronoUnit.DAYS.between(it.fom, d).toInt()
            }
        }
        val oppgitteRefusjonsdager = ps.sumOf { it.antallDagerMedRefusjon }
        val arbeidsgiverensDager = antallDagerArbeidsgiverBetalerFor(ps.first())
        arbeidsgiverdagerUtenRefusjon = min(arbeidsgiverdagerUtenRefusjon, arbeidsgiverensDager)

        if (arbeidsgiverdagerUtenRefusjon > 0) {
            oppgitteRefusjonsdager <= refusjonsdager - (arbeidsgiverensDager - arbeidsgiverdagerUtenRefusjon)
        } else {
            oppgitteRefusjonsdager <= refusjonsdager - arbeidsgiverensDager
        }
    }

class SammenhengeneArbeidsgiverPeriode : CustomConstraint

fun <E> Validator<E>.Property<Iterable<Arbeidsgiverperiode>?>.harMaksimaltOppholdMellomPerioder(maksDagerMedOpphold: Int) =
    this.validate(SammenhengeneArbeidsgiverPeriode()) {
        val sorted = it!!.sortedBy { p -> p.fom }

        if (sorted.size < 2) {
            return@validate true
        }

        for (i in 0..sorted.size - 2) {
            val gapStartDate = sorted[i].tom.plusDays(1)
            val firstDayNextPeriode = sorted[i + 1].fom
            val gapPeriod = ChronoUnit.DAYS.between(gapStartDate, firstDayNextPeriode).toInt()

            if (gapPeriod > maksDagerMedOpphold) {
                return@validate false
            }
        }

        true
    }

class IngenOverlapptomePerioderContraint : CustomConstraint

fun <E> Validator<E>.Property<Iterable<Arbeidsgiverperiode>?>.harIngenOverlappendePerioder() =
    this.validate(IngenOverlapptomePerioderContraint()) {
        !it!!.any { a ->
            it.any { b ->
                a != b && a.fom < b.tom && b.fom < a.tom
            }
        }
    }

class MaksArbeidsgiverperiodeLengdeConstraint : CustomConstraint

fun <E> Validator<E>.Property<Iterable<Arbeidsgiverperiode>?>.totalPeriodeLengdeErMaks(maksDager: Int) =
    this.validate(MaksArbeidsgiverperiodeLengdeConstraint()) { ps ->
        val sum = ps!!.map {
            ChronoUnit.DAYS.between(it.fom, it.tom.plusDays(1))
        }.sum()
        sum <= maksDager
    }

class RefusjonsdagerKanIkkeOverstigePeriodelengdenConstraint : CustomConstraint

fun <E> Validator<E>.Property<Iterable<Arbeidsgiverperiode>?>.refujonsDagerIkkeOverstigerPeriodelengder() =
    this.validate(RefusjonsdagerKanIkkeOverstigePeriodelengdenConstraint()) { ps ->
        !ps!!.any { p ->
            ChronoUnit.DAYS.between(p.fom, p.tom.plusDays(1)) < p.antallDagerMedRefusjon
        }
    }

class TomPeriodeKanIkkeHaBeloepConstraint : CustomConstraint

fun <E> Validator<E>.Property<Iterable<Arbeidsgiverperiode>?>.tomPeriodeKanIkkeHaBeloepConstraint() =
    this.validate(TomPeriodeKanIkkeHaBeloepConstraint()) { ps ->
        !ps!!.any { p ->
            p.antallDagerMedRefusjon == 0 && p.beloep > 0
        }
    }

class RefusjonsdagerInnenforGyldigPeriodeConstraint : CustomConstraint

fun <E> Validator<E>.Property<Iterable<Arbeidsgiverperiode>?>.refusjonsdagerInnenforGyldigPeriode(refusjonsdagerFom: LocalDate) =
    this.validate(RefusjonsdagerInnenforGyldigPeriodeConstraint()) { ps ->
        ps!!.all { p ->
            val validDays = ChronoUnit.DAYS.between(refusjonsdagerFom, p.tom.plusDays(1))
            (p.fom >= refusjonsdagerFom || (p.antallDagerMedRefusjon == 0 || p.antallDagerMedRefusjon <= validDays))
        }
    }

class RefusjonsdagerInnenforGjenaapningConstraint : CustomConstraint

fun <E> Validator<E>.Property<Iterable<Arbeidsgiverperiode>?>.refusjonsdatoIkkeiGjenåpning() =
    this.validate(RefusjonsdagerInnenforGjenaapningConstraint()) { ps ->
        ps!!.all { p ->
            innenforGammelPeriode(p) || innenforNyPeriode(p)
        }
    }
