package no.nav.helse.sporenstreks.integrasjon

import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.*
import no.nav.helse.sporenstreks.domene.Refusjonskrav
import no.nav.helse.sporenstreks.integrasjon.rest.brreg.BrregClient
import no.nav.helse.sporenstreks.pdf.PDFGenerator
import java.util.*

class JoarkService(val dokarkivKlient: DokarkivKlient, val brreg: BrregClient) {
    val pdfGenerator = PDFGenerator()

    fun journalfør(refusjonskrav: Refusjonskrav, callId: String): String {
        val virksomhetsNavn = runBlocking {
            brreg.getVirksomhetsNavn(refusjonskrav.virksomhetsnummer)
        }
        val base64EnkodetPdf = Base64.getEncoder().encodeToString(pdfGenerator.lagPDF(refusjonskrav, virksomhetsNavn))
        return dokarkivKlient.journalførDokument(
            JournalpostRequest(
                journalposttype = Journalposttype.INNGAAENDE,
                kanal = "NAV_NO",
                eksternReferanseId = refusjonskrav.id.toString(),
                tittel = "Refusjonskrav arbeidsgiverperiode korona",
                bruker = Bruker(
                    id = refusjonskrav.identitetsnummer,
                    idType = IdType.FNR
                ),
                avsenderMottaker = AvsenderMottaker(
                    id = refusjonskrav.virksomhetsnummer,
                    idType = IdType.ORGNR,
                    navn = virksomhetsNavn
                ),
                dokumenter = listOf(
                    Dokument(
                        brevkode = "refusjonskrav_arbeidsgiverperiode_korona",
                        tittel = "Refusjonskrav arbeidsgiverperiode korona",
                        dokumentVarianter = listOf(
                            DokumentVariant(
                                fysiskDokument = base64EnkodetPdf
                            )
                        )
                    )
                ),
                datoMottatt = refusjonskrav.opprettet.toLocalDate()
            ),
            true, callId
        ).journalpostId
    }
}
