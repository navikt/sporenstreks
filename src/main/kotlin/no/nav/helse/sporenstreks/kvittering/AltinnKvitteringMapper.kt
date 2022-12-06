package no.nav.helse.sporenstreks.kvittering

import no.altinn.schemas.services.serviceengine.correspondence._2010._10.ExternalContentV2
import no.altinn.schemas.services.serviceengine.correspondence._2010._10.InsertCorrespondenceV2
import java.time.format.DateTimeFormatter

class AltinnKvitteringMapper(val altinnTjenesteKode: String) {

    fun mapKvitteringTilInsertCorrespondence(kvittering: Kvittering): InsertCorrespondenceV2 {
        val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        val dateTimeFormatterMedKl = DateTimeFormatter.ofPattern("dd.MM.yyyy 'kl.' HH:mm")
        val dateTimeFormatterPlain = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

        val tittel = "Kvittering for krav om utvidet refusjon ved koronaviruset"

        val innhold = """
            <html>
               <head>
                   <meta charset="UTF-8">
               </head>
               <body>
                   <div class="melding">
                       <h2>Kvittering for krav om utvidet refusjon ved koronaviruset</h2>
                       <p><strong>Virksomhetsnummer</strong> ${kvittering.virksomhetsnummer}<p>
                       <p>${kvittering.tidspunkt.format(dateTimeFormatterMedKl)}</p>
                       <p></p>
                       <p>
                        Kravet vil bli behandlet raskt dersom alt er i orden. Har dere spørsmål, ring NAVs arbeidsgivertelefon <br><strong>55 55 33 36.</strong>
                        </p>
                        <p></p>
                        <h3>Dere har innrapportert følgende: </h3>
                        <table style="border-style:solid; border-color:rgba(64, 56, 50, 1); border-width:2px;">
                            <tr style="border-style:solid; border-color:rgba(64, 56, 50, 1); border-width:2px;">
                                <th style="padding:12px">Fødselsnummer</th>
                                <th style="padding:12px">Periode</th>
                                <th style="padding:12px">Antall dager</th>
                                <th style="padding:12px">Beløp</th>
                                <th style="padding:12px">Virksomhetsnummer</th>
                                <th style="padding:12px">Mottatt</th>
                            </tr>
                        ${kvittering.refusjonsListe.sorted().joinToString(separator = "") { krav ->
            krav.perioder.sorted().joinToString(
                separator = "\n"
            ) {
                val sladdetFnr = sladdFnr(krav.identitetsnummer)
                """
                                <tr>
                                <td style="padding:12px">$sladdetFnr</td>
                                <td style="padding:12px">${it.fom.format(dateFormatter)} - ${it.tom.format(dateFormatter)}</td>
                                <td style="padding:12px">${it.antallDagerMedRefusjon}</td>
                                <td style="padding:12px">${it.beloep}</td>
                                <td style="padding:12px">${krav.virksomhetsnummer}</td>
                                <td style="padding:12px">${krav.opprettet.format(dateTimeFormatterPlain)}</td>
                                </tr>
                """.trimIndent()
            }
        }}
                       </table>
                   </div>
               </body>
            </html>
        """.trimIndent()

        val meldingsInnhold = ExternalContentV2()
            .withLanguageCode("1044")
            .withMessageTitle(tittel)
            .withMessageBody(innhold)
            .withMessageSummary("Kvittering for krav om utvidet refusjon ved koronaviruset")

        return InsertCorrespondenceV2()
            .withAllowForwarding(false)
            .withReportee(kvittering.virksomhetsnummer)
            .withMessageSender("NAV (Arbeids- og velferdsetaten)")
            .withServiceCode(altinnTjenesteKode)
            .withServiceEdition("1")
            .withContent(meldingsInnhold)
    }

    fun sladdFnr(fnr: String): String {
        return fnr.take(6) + "*****"
    }
}
