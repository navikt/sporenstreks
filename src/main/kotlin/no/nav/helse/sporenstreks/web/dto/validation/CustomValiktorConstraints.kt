package no.nav.helse.sporenstreks.web.dto.validation

import no.nav.helse.sporenstreks.domene.Arbeidsgiverperiode
import org.valiktor.Constraint
import org.valiktor.Validator
import java.time.LocalDate
import java.time.Period
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
fun <E> Validator<E>.Property<Iterable<Arbeidsgiverperiode>?>.arbeidsgiverBetalerForDager(arbeidsgiverensDager: Int, d: LocalDate) =
        this.validate(RefusjonsDagerConstraint()) { ps ->
            var refusjonsdager = 0
            var arbeidsgiverdagerUtenRefusjon = 0
            ps!!.forEach() {
                if (it.fom >= d) {
                  refusjonsdager += Period.between(it.fom, it.tom.plusDays(1)).days
                } else if (it.tom < d){
                    arbeidsgiverdagerUtenRefusjon += Period.between(it.fom, it.tom.plusDays(1)).days
                } else {
                    refusjonsdager += Period.between(d, it.tom.plusDays(1)).days
                    arbeidsgiverdagerUtenRefusjon += Period.between(it.fom, d).days
                }
            }
            val oppgitteRefusjonsdager = ps!!.sumBy { it.antallDagerMedRefusjon }

            arbeidsgiverdagerUtenRefusjon = min(arbeidsgiverdagerUtenRefusjon, arbeidsgiverensDager)

            if (arbeidsgiverdagerUtenRefusjon > 0) {
                oppgitteRefusjonsdager <= refusjonsdager - (arbeidsgiverensDager -arbeidsgiverdagerUtenRefusjon)
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
                val gapPeriod = Period.between(gapStartDate, firstDayNextPeriode)

                if (gapPeriod.days > maksDagerMedOpphold) {
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

class RefusjonsbeløpKanIkkeOverstigeGrunnbeløpConstraint : CustomConstraint
fun <E> Validator<E>.Property<Iterable<Arbeidsgiverperiode>?>.refusjonsbeløpKanIkkeOverstigeGrunnbeløp(g: Double) =
        this.validate(RefusjonsbeløpKanIkkeOverstigeGrunnbeløpConstraint()) { ps ->
            ps!!.all { p ->
                ((p.antallDagerMedRefusjon * (g / 260.0)) >= p.beloep)
            }
        }

class RefusjonsdagerInnenforGyldigPeriodeConstraint : CustomConstraint
fun <E> Validator<E>.Property<Iterable<Arbeidsgiverperiode>?>.refusjonsdagerInnenforGyldigPeriode(d: LocalDate) =
        this.validate(RefusjonsdagerInnenforGyldigPeriodeConstraint()) { ps ->
            ps!!.all { p ->
                val validDays = ChronoUnit.DAYS.between(d, p.tom.plusDays(1))
                p.antallDagerMedRefusjon <= validDays
            }
        }
