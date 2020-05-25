package no.nav.helse.sporenstreks.auth

import no.nav.helse.sporenstreks.domene.AltinnOrganisasjon

interface Authorizer {
    /**
     * Sjekker om den angitte identiteten har rettighet til å se refusjoner for den angitte arbeidsgiverId
     * En arbeidsgiverId kan være en virksomhet, en hovedenhet, et identitetsnummer på en privatperson eller et
     * organisasjonsledd.
     */
    fun hasAccess(identitetsnummer: String, arbeidsgiverId: String): Boolean
}

/**
 * Standard Authorizer som sjekker at
 *  * Den angitte brukeren har rettigheter til den angitte arbeidsgiverIDen
 *  * Den angitte arbeidsgiver IDen er en underenhet
 */
class DefaultAuthorizer(private val authListRepo: AuthorizationsRepository) : Authorizer {
    override fun hasAccess(identitetsnummer: String, arbeidsgiverId: String): Boolean {
        return authListRepo.hentOrgMedRettigheterForPerson(identitetsnummer)
                .any {
                    it.organizationNumber == arbeidsgiverId && it.parentOrganizationNumber != null
                }
    }
}

interface AuthorizationsRepository {
    fun hentOrgMedRettigheterForPerson(identitetsnummer: String): Set<AltinnOrganisasjon>
}