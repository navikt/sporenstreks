package no.nav.helse.spion.auth

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.javafaker.Faker
import no.nav.helse.spion.domene.AltinnOrganisasjon
import java.time.LocalDateTime
import javax.sql.DataSource

/**
 * Henter et tilfeldig sett arbeidsgivere fra perioder i databasen
 * og lager en ACL på bakgrunn av denne. Dette gjør at hvilkensomhelst identitet som logger inn via OIDC-stuben i dev
 * vil få tilganger til noen arbeidsgivere som har perioder i databasen
 */
class DynamicMockAuthRepo(private val om: ObjectMapper, private val dataSource: DataSource) : AuthorizationsRepository {
    val cache = mutableMapOf<String, Pair<LocalDateTime, Set<AltinnOrganisasjon>>>()


    override fun hentOrgMedRettigheterForPerson(identitetsnummer: String): Set<AltinnOrganisasjon> {
        return emptySet()
    }
}
