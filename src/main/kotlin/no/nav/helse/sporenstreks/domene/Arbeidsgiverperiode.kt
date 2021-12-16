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

        // Periode 01.12.2021 - 30.06.2022
        val refusjonFraDato = LocalDate.of(2021, 12, 1)
        val refusjonTilDato = LocalDate.of(2022, 6, 30)
        val arbeidsgiverBetalerForDager = 5
        val antallMånederTilStengt: Long = 3

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
