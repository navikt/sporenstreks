package no.nav.helse.sporenstreks.domene

import java.time.LocalDate

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
    }
}
