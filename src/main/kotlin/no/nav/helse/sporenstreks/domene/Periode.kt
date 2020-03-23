package no.nav.helse.spion.domene

import java.time.LocalDate

data class Periode(
        val fom: LocalDate,
        val tom: LocalDate
)