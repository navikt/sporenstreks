package no.nav.helse.sporenstreks.domene

import java.time.LocalDate

data class Periode(
        val fom: LocalDate,
        val tom: LocalDate
)