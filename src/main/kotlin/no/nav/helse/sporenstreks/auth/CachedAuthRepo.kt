package no.nav.helse.sporenstreks.auth

import no.nav.helse.sporenstreks.domene.AltinnOrganisasjon
import no.nav.helse.sporenstreks.utils.SimpleHashMapCache
import org.slf4j.LoggerFactory
import java.time.Duration

class CachedAuthRepo(private val sourceRepo: AuthorizationsRepository) : AuthorizationsRepository {
    val cache = SimpleHashMapCache<Set<AltinnOrganisasjon>>(Duration.ofMinutes(60), 100)
    val logger = LoggerFactory.getLogger(CachedAuthRepo::class.java)

    override fun hentOrgMedRettigheterForPerson(identitetsnummer: String): Set<AltinnOrganisasjon> {
        if(cache.hasValidCacheEntry(identitetsnummer)) {
            logger.debug("Cache hit")
            return cache.get(identitetsnummer)
        }
        logger.debug("Cache miss")

        val acl = sourceRepo.hentOrgMedRettigheterForPerson(identitetsnummer)
        cache.put(identitetsnummer, acl)
        return acl
    }
}