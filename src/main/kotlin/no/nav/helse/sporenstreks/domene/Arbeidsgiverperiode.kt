package no.nav.helse.sporenstreks.domene

import java.time.LocalDate

data class Arbeidsgiverperiode(
    val fom: LocalDate,
    val tom: LocalDate,
    val antallDagerMedRefusjon: Int,
    val beloep: Double
) : Comparable<Arbeidsgiverperiode> {
    companion object {
        // Gammel periode 2020.03.16 - 2021.10.1
        val refusjonFraDatoGammelPeriode = LocalDate.of(2020, 3, 16)
        val refusjonTilDatoGammelPeriode = LocalDate.of(2021, 10, 1)
        val arbeidsgiverBetalerForDagerGammelPeriode = 3
        val antallMånederTilStengtGammelPeriode: Long = 3

        // Ny periode 2021.11.30 - 2022.07.1
        val refusjonFraDatoNyPeriode = LocalDate.of(2021, 11, 30)
        val refusjonTilDatoNyPeriode = LocalDate.of(2022, 7, 1)
        val arbeidsgiverBetalerForDagerNyPeriode = 5

        val maksOppholdMellomPerioder = 16
        val maksimalAGPLengde = 16
    }

    override fun compareTo(other: Arbeidsgiverperiode): Int {
        if (other.fom.isAfter(fom))
            return -1
        if (other.fom.isBefore(fom))
            return 1
        return 0
    }
}
