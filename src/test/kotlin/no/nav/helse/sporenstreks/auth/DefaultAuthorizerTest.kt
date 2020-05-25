package no.nav.helse.sporenstreks.auth

import io.mockk.every
import io.mockk.mockk
import no.nav.helse.sporenstreks.domene.AltinnOrganisasjon
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DefaultAuthorizerTest {
    lateinit var authorizer: DefaultAuthorizer

    private val subjectWithAccess = "02038509876"
    private val doesNotHaveAccess = "xxxxxxxxxx"

    @BeforeEach
    internal fun setUp() {
        val authRepoMock = mockk<AuthorizationsRepository>()
        every { authRepoMock.hentOrgMedRettigheterForPerson(subjectWithAccess) } returns setOf(
                AltinnOrganisasjon("Juridisk enhet", "Enterprise", organizationNumber = "123"),
                AltinnOrganisasjon("Undernehet", "Business", organizationNumber = "567", parentOrganizationNumber = "123"),
                AltinnOrganisasjon("person ", "Person", socialSecurityNumber = "01028454321")
        )

        every { authRepoMock.hentOrgMedRettigheterForPerson(doesNotHaveAccess) } returns setOf()

        authorizer = DefaultAuthorizer(authRepoMock)
    }

    @Test
    internal fun `access denied for Juridisk Enhet`() {
        assertThat(authorizer.hasAccess(subjectWithAccess, "123")).isFalse()
    }

    @Test
    internal fun `access granted for Underenhet`() {
        assertThat(authorizer.hasAccess(subjectWithAccess, "567")).isTrue()
    }

    @Test
    internal fun `org numbers Not In the List Is Denied`() {
        assertThat(authorizer.hasAccess(subjectWithAccess, "666")).isFalse()
    }

    @Test
    internal fun `no Access Rights Are Denied`() {
        assertThat(authorizer.hasAccess(doesNotHaveAccess, "123")).isFalse()
    }

    @Test
    internal fun `socialSecurityNumber is not supported`() {
        assertThat(authorizer.hasAccess(subjectWithAccess, "01028454321")).isFalse()
    }
}