package no.nav.helse.sporenstreks.pdf

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType0Font
import java.io.ByteArrayOutputStream
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Kvittering {

    private val FONT_SIZE = 11f
    private val LINE_HEIGHT = 15f
    private val MARGIN_X = 40f
    private val MARGIN_Y = 40f
    private val FONT_NAME = "fonts/SourceSansPro-Regular.ttf"
    val TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
    val DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    val NUMBER_FORMAT = DecimalFormat("#,###.00")

    fun lagPDF(innhold: Innhold) : ByteArray {
        val doc = PDDocument()
        val page = PDPage()
        val font = PDType0Font.load(doc , this::class.java.classLoader.getResource(FONT_NAME).openStream())
        doc.addPage(page)
        val contentStream = PDPageContentStream(doc, page)
        contentStream.beginText()
        contentStream.setFont(font, FONT_SIZE)
        val mediaBox = page.mediaBox
        val startX = mediaBox.lowerLeftX + MARGIN_X
        val startY = mediaBox.upperRightY - MARGIN_Y

        contentStream.newLineAtOffset(startX, startY)
        contentStream.showText("Navn: ${innhold.navn}")
        contentStream.newLineAtOffset(0F, -LINE_HEIGHT*2)
        contentStream.showText("Fødselsnummer: ${innhold.refusjonskrav.identitetsnummer}")
        contentStream.newLineAtOffset(0F, -LINE_HEIGHT)
        contentStream.showText("Virksomhetsnummer: ${innhold.refusjonskrav.virksomhetsnummer}")

        contentStream.newLineAtOffset(0F, -LINE_HEIGHT)
        contentStream.showText("Refusjonsbeløp: ${NUMBER_FORMAT.format(innhold.refusjonskrav.beloep)} kroner")

        contentStream.newLineAtOffset(0F, -LINE_HEIGHT*2)
        contentStream.showText("Perioder:")
        innhold.refusjonskrav.perioder.forEach {
            contentStream.newLineAtOffset(0F, -LINE_HEIGHT)
            contentStream.showText("Fra: ${DATE_FORMAT.format(it.fom)}   Til: ${DATE_FORMAT.format(it.tom)}    Antall dager: ${it.antallDagerMedRefusjon}")
        }


        contentStream.newLineAtOffset(0F, -LINE_HEIGHT*5)
        contentStream.showText("Referansenummer: ${innhold.refusjonskrav.referansenummer}")
        contentStream.newLineAtOffset(0F, -LINE_HEIGHT*2)
        contentStream.showText("Opprettet: ${TIMESTAMP_FORMAT.format(LocalDateTime.now())}")

        contentStream.endText()
        contentStream.close()
        val out = ByteArrayOutputStream()
        doc.save(out)
        return out.toByteArray()
    }

}