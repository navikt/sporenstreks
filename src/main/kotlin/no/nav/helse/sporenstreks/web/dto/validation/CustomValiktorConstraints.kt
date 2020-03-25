package no.nav.helse.sporenstreks.web.dto.validation

import no.nav.helse.sporenstreks.web.dto.Arbeidsgiverperiode
import org.valiktor.Constraint
import org.valiktor.Validator
import java.time.Duration
import java.time.Period

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
fun <E> Validator<E>.Property<Iterable<Arbeidsgiverperiode>?>.sumAntallDagerMindreEnnEllerLik(maksDager: Int) =
        this.validate(RefusjonsDagerConstraint()) { ps -> ps!!.sumBy { it.antallDagerMedRefusjon } <= maksDager }


class SammenhengeneArbeidsgiverPeriode : CustomConstraint
fun <E> Validator<E>.Property<Iterable<Arbeidsgiverperiode>?>.harMaksimaltOppholdMellomPerioder(maksDagerMedOpphold: Int) =
        this.validate(SammenhengeneArbeidsgiverPeriode())  {
            val sorted = it!!.sortedBy { p -> p.fom }

            if (sorted.size < 2) {
                return@validate true
            }

            for (i in 0..sorted.size-2) {
                val gapStartDate = sorted[i].tom.plusDays(1)
                val firstDayNextPeriode = sorted[i+1].fom
                val gapPeriod = Period.between(gapStartDate, firstDayNextPeriode)

                if (gapPeriod.days > maksDagerMedOpphold) {
                    return@validate false
                }
            }

            true
        }

class IngenOverlapptomePerioderContraint : CustomConstraint
fun <E> Validator<E>.Property<Iterable<Arbeidsgiverperiode>?>.harIngenOverlappendePerioder() =
        this.validate(IngenOverlapptomePerioderContraint())  {
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
                Period.between(it.fom, it.tom.plusDays(1))
            }.sumBy { it.days }
            sum <= maksDager
        }


class RefusjonsdagerKanIkkeOverstigePeriodelengdenConstraint : CustomConstraint
fun <E> Validator<E>.Property<Iterable<Arbeidsgiverperiode>?>.refujonsDagerIkkeOverstigerPeriodelengder() =
        this.validate(RefusjonsdagerKanIkkeOverstigePeriodelengdenConstraint()) { ps ->
            !ps!!.any { p ->
              Period.between(p.fom, p.tom.plusDays(1)).days < p.antallDagerMedRefusjon
            }
        }