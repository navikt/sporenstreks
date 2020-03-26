package no.nav.helse.sporenstreks.integrasjon

import no.nav.helse.sporenstreks.dokument.PdfGenerator
import no.nav.helse.sporenstreks.domene.Refusjonskrav
import no.nav.helse.sporenstreks.integrasjon.rest.dokarkiv.DokarkivKlient

class JoarkService(val pdfGenerator: PdfGenerator,
                   val dokarkivKlient: DokarkivKlient) {
    fun journalfør(refusjonskrav: Refusjonskrav): String {
        val base64EnkodetPdf = pdfGenerator.lagBase64Pdf(refusjonskrav)
        return dokarkivKlient.journalførDokument(base64EnkodetPdf, refusjonskrav)
    }
}