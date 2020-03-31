package no.nav.helse.sporenstreks.integrasjon.rest.dokarkiv

data class JournalpostResponse(
        val journalpostId: String,
        val journalpostFerdigstilt: Boolean,
        val journalStatus: String,
        val melding: String? = null,
        val dokumenter: List<DokumentResponse>
)

data class DokumentResponse(
        val brevkode: String?,
        val dokumentInfoId: Int?,
        val tittel: String?
)