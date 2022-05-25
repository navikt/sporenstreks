package no.nav.helse.sporenstreks.integrasjon.rest

import no.nav.helse.arbeidsgiver.integrasjoner.aareg.AaregArbeidsforholdClient
import no.nav.helse.arbeidsgiver.integrasjoner.aareg.Ansettelsesperiode
import no.nav.helse.arbeidsgiver.integrasjoner.aareg.Arbeidsforhold
import no.nav.helse.arbeidsgiver.integrasjoner.aareg.Arbeidsgiver
import no.nav.helse.arbeidsgiver.integrasjoner.aareg.Opplysningspliktig
import no.nav.helse.arbeidsgiver.integrasjoner.aareg.Periode
import no.nav.helse.sporenstreks.domene.Arbeidsgiverperiode
import java.time.LocalDate

class MockAaregArbeidsforholdClient : AaregArbeidsforholdClient {
    override suspend fun hentArbeidsforhold(ident: String, callId: String): List<Arbeidsforhold> =
        listOf(
            Arbeidsforhold(
                Arbeidsgiver("test", "805824352"),
                Opplysningspliktig("Juice", "805824352"),
                emptyList(),
                Ansettelsesperiode(Periode(LocalDate.of(2021, 1, 12), LocalDate.of(2021, 8, 14))),
                Arbeidsgiverperiode.refusjonFraDato.atStartOfDay()
            ),
            Arbeidsforhold(
                Arbeidsgiver("test", "810007842"),
                Opplysningspliktig("Juice", "810007702"),
                emptyList(),
                Ansettelsesperiode(Periode(LocalDate.MIN, null)),
                Arbeidsgiverperiode.refusjonFraDato.atStartOfDay()
            ),
            Arbeidsforhold(
                Arbeidsgiver("test", "910098896"),
                Opplysningspliktig("Juice", "910098896"),
                emptyList(),
                Ansettelsesperiode(Periode(LocalDate.MIN, null)),
                Arbeidsgiverperiode.refusjonFraDato.atStartOfDay()
            )
        )
}
