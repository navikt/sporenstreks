package no.nav.helse.sporenstreks.integrasjon.rest

import no.nav.helse.arbeidsgiver.integrasjoner.aareg.AaregArbeidsforholdClient
import no.nav.helse.arbeidsgiver.integrasjoner.aareg.Ansettelsesperiode
import no.nav.helse.arbeidsgiver.integrasjoner.aareg.Arbeidsforhold
import no.nav.helse.arbeidsgiver.integrasjoner.aareg.Arbeidsgiver
import no.nav.helse.arbeidsgiver.integrasjoner.aareg.Opplysningspliktig
import no.nav.helse.arbeidsgiver.integrasjoner.aareg.Periode
import no.nav.helse.sporenstreks.domene.Arbeidsgiverperiode

class MockAaregArbeidsforholdClient : AaregArbeidsforholdClient {
    override suspend fun hentArbeidsforhold(ident: String, callId: String): List<Arbeidsforhold> =
        listOf(
            Arbeidsforhold(
                Arbeidsgiver("test", "810007842"),
                Opplysningspliktig("Juice", "810007702"),
                emptyList(),
                Ansettelsesperiode(Periode(Arbeidsgiverperiode.refusjonFraDato, null)),
                Arbeidsgiverperiode.refusjonFraDato.atStartOfDay()
            ),
            Arbeidsforhold(
                Arbeidsgiver("test", "910098896"),
                Opplysningspliktig("Juice", "910098896"),
                emptyList(),
                Ansettelsesperiode(Periode(Arbeidsgiverperiode.refusjonFraDato, null)),
                Arbeidsgiverperiode.refusjonFraDato.atStartOfDay()
            )
        )
}
