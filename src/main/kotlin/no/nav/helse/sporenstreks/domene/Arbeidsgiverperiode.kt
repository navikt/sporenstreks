package no.nav.helse.sporenstreks.domene

import java.time.LocalDate

data class Arbeidsgiverperiode(
    val fom: LocalDate,
    val tom: LocalDate,
    val antallDagerMedRefusjon: Int,
    val beloep: Double
) : Comparable<Arbeidsgiverperiode> {
    companion object {
        // Gammel periode 01.01.2022 - 30.06.2022
        val refusjonFraDato = LocalDate.of(2022, 1, 1)
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
