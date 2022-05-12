package no.nav.helse.sporenstreks.utils

import java.time.LocalDate

fun LocalDate.isAfterOrEqual(other: LocalDate): Boolean =
    this == other || this.isAfter(other)

fun LocalDate.isBeforeOrEqual(other: LocalDate): Boolean =
    this == other || this.isBefore(other)
