package no.nav.helse.sporenstreks.domene

import java.time.LocalDate

data class Arbeidsgiverperiode(
    val fom: LocalDate,
    val tom: LocalDate,
    val antallDagerMedRefusjon: Int,
    val beloep: Double
) : Comparable<Arbeidsgiverperiode> {
    companion object {
        val refusjonFraDato = LocalDate.of(2020, 3, 16)
        val maksOppholdMellomPerioder = 16
        val maksimalAGPLengde = 16
        val arbeidsgiverBetalerForDager = 3
    }

    fun innenforGammelPeriode(arbeidsgiverPeriode: Arbeidsgiverperiode, now: LocalDate = LocalDate.now()): Boolean {
        val fraDato = now.minusMonths(6)
        val tilDato = LocalDate.of(2021, 10, 1)
        return arbeidsgiverPeriode.fom.isAfter(fraDato) &&
            arbeidsgiverPeriode.tom.isBefore(tilDato)
    }

    fun innenforNyPeriode(arbeidsgiverPeriode: Arbeidsgiverperiode): Boolean {
        val fraDato = LocalDate.of(2021, 11, 30)
        val tilDato = LocalDate.of(2022, 7, 1)
        return arbeidsgiverPeriode.fom.isAfter(fraDato) &&
            arbeidsgiverPeriode.tom.isBefore(tilDato)
    }

    override fun compareTo(other: Arbeidsgiverperiode): Int {
        if (other.fom.isAfter(fom))
            return -1
        if (other.fom.isBefore(fom))
            return 1
        return 0
    }
}
