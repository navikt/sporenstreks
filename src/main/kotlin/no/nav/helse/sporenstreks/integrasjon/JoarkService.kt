package no.nav.helse.sporenstreks.integrasjon

import no.nav.helse.sporenstreks.dokument.PdfGenerator
import no.nav.helse.sporenstreks.domene.Refusjonskrav
import no.nav.helse.sporenstreks.integrasjon.rest.JournalKlient

class JoarkService(val pdfGenerator: PdfGenerator,
                   val journalKlient: JournalKlient) {
    fun journalfør(refusjonskrav: Refusjonskrav): String {
        val base64EnkodetPdf = pdfGenerator.lagBase64Pdf(refusjonskrav)
        return journalKlient.journalførDokument(base64EnkodetPdf)
    }
}