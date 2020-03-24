package no.nav.helse.sporenstreks.web.dto

import no.nav.helse.sporenstreks.domene.Periode
import java.math.BigDecimal
import java.time.LocalDate

data class SkjemaDto(
        val fnr: String,
        val arbeidsgiverId: String,
        val førsteFraværsdag: LocalDate,
        val arbeidsgiverPerioder: List<Periode>,
        val beløp: BigDecimal,
        val dagerDetKrevesRefusjonFor: List<LocalDate>
)