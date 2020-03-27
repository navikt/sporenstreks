package no.nav.helse.sporenstreks.integrasjon.rest.dokarkiv

import java.time.LocalDate

data class JournalpostRequest(
        val tema: String = "SYK",
        val bruker: Bruker,
        val avsenderMottaker: AvsenderMottaker,
        val tittel: String = "Søknad om Refusjon i forbindelse med Corona", //TODO
        val dokumenter: List<Dokument>,
        val sak: Sak = Sak(),
        val datoMottatt: LocalDate = LocalDate.now() //TODO
)

data class Dokument(
        val brevkode: String = "CORONAREFUSJONSØKNAD", //TODO
        val dokumentKategori: String = "SOK", //TODO
        val tittel: String = "Søknad refusjon corona", //TODO
        val dokumentVarianter: List<DokumentVariant>
)

data class DokumentVariant(
        val filnavn: String = "coronasøknad.pdf", //TODO
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