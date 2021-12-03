package no.nav.helse.sporenstreks.pdf

import no.nav.helse.sporenstreks.domene.Refusjonskrav
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType0Font
import java.io.ByteArrayOutputStream
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class PDFGenerator {

    private val FONT_SIZE = 11f
    private val LINE_HEIGHT = 15f
    private val MARGIN_X = 40f
    private val MARGIN_Y = 40f
    private val FONT_NAME = "fonts/SourceSansPro-Regular.ttf"
    val TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
    val DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    val NUMBER_FORMAT = DecimalFormat("#,###.00")

    fun lagPDF(refusjonskrav: Refusjonskrav): ByteArray {
        val doc = PDDocument()
        val page = PDPage()
        val font = PDType0Font.load(doc, this::class.java.classLoader.getResource(FONT_NAME).openStream())
        doc.addPage(page)
        val contentStream = PDPageContentStream(doc, page)
        contentStream.beginText()
        contentStream.setFont(font, FONT_SIZE)
        val mediaBox = page.mediaBox
        val startX = mediaBox.lowerLeftX + MARGIN_X
        val startY = mediaBox.upperRightY - MARGIN_Y
        contentStream.newLineAtOffset(startX, startY)
        contentStream.showText("Fødselsnummer: ${refusjonskrav.identitetsnummer}")
        contentStream.newLineAtOffset(0F, -LINE_HEIGHT)
        contentStream.showText("Virksomhetsnummer: ${refusjonskrav.virksomhetsnummer}")
        contentStream.newLineAtOffset(0F, -LINE_HEIGHT * 2)
        contentStream.showText("Perioder:")
        refusjonskrav.perioder.forEach {
            contentStream.newLineAtOffset(0F, -LINE_HEIGHT)
            contentStream.showText("Fra: ${DATE_FORMAT.format(it.fom)}    Til: ${DATE_FORMAT.format(it.tom)}    Antall dager: ${it.antallDagerMedRefusjon}    Refusjonsbeløp: ${NUMBER_FORMAT.format(it.beloep)}")
        }
        contentStream.newLineAtOffset(0F, -LINE_HEIGHT * 5)
        contentStream.showText("Referansenummer: ${refusjonskrav.referansenummer}")
        contentStream.newLineAtOffset(0F, -LINE_HEIGHT * 2)
        contentStream.showText("Opprettet: ${TIMESTAMP_FORMAT.format(LocalDateTime.now())}")
        contentStream.endText()
        contentStream.close()
        val out = ByteArrayOutputStream()
        doc.save(out)
        val ba = out.toByteArray()
        doc.close()
        return ba
    }
}
