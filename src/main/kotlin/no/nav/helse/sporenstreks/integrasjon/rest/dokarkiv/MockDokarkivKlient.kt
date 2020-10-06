package no.nav.helse.sporenstreks.integrasjon.rest.dokarkiv

import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.DokarkivKlient
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.JournalpostRequest
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.JournalpostResponse

class MockDokarkivKlient : DokarkivKlient {
    override fun journalførDokument(journalpost: JournalpostRequest, forsoekFerdigstill: Boolean, callId: String): JournalpostResponse {
        return JournalpostResponse("id", true, "J", dokumenter = emptyList())
    }
}
