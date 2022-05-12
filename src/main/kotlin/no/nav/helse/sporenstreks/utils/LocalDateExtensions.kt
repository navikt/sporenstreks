package no.nav.helse.sporenstreks.utils

import java.time.LocalDate

fun LocalDate.isAfterOrEqual(other: LocalDate): Boolean =
    this.isEqual(other)|| this.isAfter(other)

fun LocalDate.isBeforeOrEqual(other: LocalDate): Boolean =
    this.isEqual(other) || this.isBefore(other)
