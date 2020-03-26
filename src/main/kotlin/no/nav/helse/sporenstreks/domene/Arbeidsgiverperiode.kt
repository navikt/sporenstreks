package no.nav.helse.sporenstreks.domene

import java.time.LocalDate

data class Arbeidsgiverperiode(
        val fom: LocalDate,
        val tom: LocalDate,
        val antallDagerMedRefusjon: Int,
        val beloep: Double
)