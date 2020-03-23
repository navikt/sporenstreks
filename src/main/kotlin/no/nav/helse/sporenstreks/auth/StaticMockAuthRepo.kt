package no.nav.helse.spion.auth

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import loadFromResources
import no.nav.helse.spion.domene.AltinnOrganisasjon
import no.nav.helse.spion.selfcheck.HealthCheck
import no.nav.helse.spion.selfcheck.HealthCheckType

class StaticMockAuthRepo(om: ObjectMapper) : AuthorizationsRepository, HealthCheck {
    override val healthCheckType = HealthCheckType.READYNESS

    private var acl: Set<AltinnOrganisasjon> = setOf(AltinnOrganisasjon("Kjellesen AS", "Enterprise", null, "AS", "1", null, null))
    var failSelfCheck = false

    init {
        "mock-data/altinn/organisasjoner-med-rettighet.json".loadFromResources().let {
            acl = om.readValue<Set<AltinnOrganisasjon>>(it)
        }
    }

    override fun hentOrgMedRettigheterForPerson(identitetsnummer: String): Set<AltinnOrganisasjon> {
        return acl
    }

    fun setAccessList(acl: Set<AltinnOrganisasjon>) {
        this.acl = acl
    }

    override suspend fun doHealthCheck() {
        if (failSelfCheck) throw Error("Feiler selfchecken")
    }
}
