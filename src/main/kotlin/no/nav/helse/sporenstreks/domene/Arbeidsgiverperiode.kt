package no.nav.helse.sporenstreks.domene

import java.time.LocalDate
import java.time.Period
import kotlin.math.min

data class Arbeidsgiverperiode(
        val fom: LocalDate,
        val tom: LocalDate,
        val antallDagerMedRefusjon: Int,
        val beloep: Double
) {
    companion object {
        val refusjonFraDato = LocalDate.of(2020, 3, 16)
        val maksOppholdMellomPerioder = 16
        val maksimalAGPLengde = 16
        val arbeidsgiverBetalerForDager = 3

        fun maxAntallDagerMedRefusjon(fom: LocalDate, tom: LocalDate): Int {
            var refusjonsdager = 0
            var arbeidsgiverdagerUtenRefusjon = 0

            if (fom >= refusjonFraDato) {
                refusjonsdager += Period.between(fom, tom.plusDays(1)).days
            } else if (tom < refusjonFraDato){
                arbeidsgiverdagerUtenRefusjon += Period.between(fom, tom.plusDays(1)).days
            } else {
                refusjonsdager += Period.between(refusjonFraDato, tom.plusDays(1)).days
                arbeidsgiverdagerUtenRefusjon += Period.between(fom, refusjonFraDato).days
            }

            arbeidsgiverdagerUtenRefusjon = min(arbeidsgiverdagerUtenRefusjon, arbeidsgiverBetalerForDager)

            return if (arbeidsgiverdagerUtenRefusjon > 0) {
                refusjonsdager - (arbeidsgiverBetalerForDager -arbeidsgiverdagerUtenRefusjon)
            } else {
                refusjonsdager - arbeidsgiverBetalerForDager
            }
        }
    }
}
