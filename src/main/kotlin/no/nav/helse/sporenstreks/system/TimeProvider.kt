package no.nav.helse.sporenstreks.system

import java.time.LocalDateTime

interface TimeProvider {
    fun now(): LocalDateTime = LocalDateTime.now()
}