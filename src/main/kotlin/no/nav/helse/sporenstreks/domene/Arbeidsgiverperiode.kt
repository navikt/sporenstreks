package no.nav.helse.sporenstreks.domene

import java.time.LocalDate

data class Arbeidsgiverperiode(
        val fom: LocalDate,
        val tom: LocalDate,
        val antallDagerMedRefusjon: Int,
        val beloep: Double
): Comparable<Arbeidsgiverperiode> {
    companion object {
        val refusjonFraDato = LocalDate.of(2020, 3, 16)
        val maksOppholdMellomPerioder = 16
        val maksimalAGPLengde = 16
        val arbeidsgiverBetalerForDager = 3
    }

    override fun compareTo(other: Arbeidsgiverperiode): Int {
        if(other.fom.isAfter(fom))
            return -1
        if(other.fom.isBefore(fom))
            return 1
        return 0
    }
}
