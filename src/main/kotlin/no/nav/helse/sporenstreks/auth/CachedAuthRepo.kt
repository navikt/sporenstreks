package no.nav.helse.sporenstreks.auth

import no.nav.helse.sporenstreks.domene.AltinnOrganisasjon
import no.nav.helse.sporenstreks.utils.SimpleHashMapCache
import java.time.Duration

class CachedAuthRepo(private val sourceRepo: AuthorizationsRepository) : AuthorizationsRepository {
    val cache = SimpleHashMapCache<Set<AltinnOrganisasjon>>(Duration.ofMinutes(60), 100)

    override fun hentOrgMedRettigheterForPerson(identitetsnummer: String): Set<AltinnOrganisasjon> {
        if(cache.hasValidCacheEntry(identitetsnummer)) return cache.get(identitetsnummer)
        
        val acl = sourceRepo.hentOrgMedRettigheterForPerson(identitetsnummer)
        cache.put(identitetsnummer, acl)
        return acl
    }
}