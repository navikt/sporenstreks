package no.nav.helse.sporenstreks.integrasjon

import no.nav.helse.sporenstreks.domene.Refusjonskrav
import no.nav.helse.sporenstreks.integrasjon.rest.dokarkiv.DokarkivKlient
import no.nav.helse.sporenstreks.pdf.PDFGenerator
import java.util.*

class JoarkService(val dokarkivKlient: DokarkivKlient) {

    val pdfGenerator = PDFGenerator()

    fun journalfør(refusjonskrav: Refusjonskrav, callId: String): String {
        val base64EnkodetPdf = Base64.getEncoder().encodeToString(pdfGenerator.lagPDF(refusjonskrav))
        return dokarkivKlient.journalførDokument(base64EnkodetPdf, refusjonskrav, callId)
    }
}