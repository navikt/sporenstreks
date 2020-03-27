package no.nav.helse.sporenstreks.integrasjon.rest.dokarkiv

import java.time.LocalDate

data class JournalpostRequest(
        val tema: String = "SYK",
        val bruker: Bruker,
        val avsenderMottaker: AvsenderMottaker,
        val tittel: String = "Refusjonskrav arbeidsgiverperiode korona",
        val dokumenter: List<Dokument>,
        val sak: Sak = Sak(),
        val datoMottatt: LocalDate = LocalDate.now()
)

data class Dokument(
        val brevkode: String = "refusjonskrav_arbeidsgiverperiode_korona",
        val tittel: String = "Refusjonskrav arbeidsgiverperiode korona",
        val dokumentVarianter: List<DokumentVariant>
)

data class DokumentVariant(
        val filtype: String = "PDFA",
        val fysiskDokument: String,
        val variantFormat: String = "ARKIV"
)

data class Bruker(
        val id: String,
        val idType: String = "FNR"
)

data class AvsenderMottaker(
        val id: String,
        val idType: String = "ORGNR",
        val navn: String
)

data class Sak(
        val sakstype: String = "GENERELL_SAK"
)