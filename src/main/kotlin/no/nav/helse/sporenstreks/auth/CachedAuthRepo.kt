package no.nav.helse.sporenstreks.auth

import no.nav.helse.arbeidsgiver.integrasjoner.altinn.AltinnOrganisasjon
import no.nav.helse.arbeidsgiver.utils.SimpleHashMapCache
import no.nav.helse.arbeidsgiver.web.auth.AltinnOrganisationsRepository
import org.slf4j.LoggerFactory
import java.time.Duration

class CachedAuthRepo(private val sourceRepo: AltinnOrganisationsRepository) : AltinnOrganisationsRepository {
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