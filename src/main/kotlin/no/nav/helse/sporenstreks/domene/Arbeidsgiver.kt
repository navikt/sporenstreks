package no.nav.helse.sporenstreks.domene

data class Arbeidsgiver(
        val navn: String,
        val organisasjonsnummer: String?,
        val arbeidsgiverId: String
)