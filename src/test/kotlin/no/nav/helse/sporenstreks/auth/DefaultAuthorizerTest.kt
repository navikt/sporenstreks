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
                AltinnOrganisasjon("test", "Enterprise", organizationNumber = "123"),
                AltinnOrganisasjon("test 2", "Enterprise", organizationNumber = "567"),
                AltinnOrganisasjon("person ", "Person", socialSecurityNumber = "01028454321")
        )

        every { authRepoMock.hentOrgMedRettigheterForPerson(doesNotHaveAccess) } returns setOf()

        authorizer = DefaultAuthorizer(authRepoMock)
    }

    @Test
    internal fun `access Is Given To Orgnumber In the Access List`() {
        assertThat(authorizer.hasAccess(subjectWithAccess, "123")).isTrue()
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
    internal fun `It is valid to have access to an altinn socialSecurityNumber`() {
        assertThat(authorizer.hasAccess(subjectWithAccess, "01028454321")).isTrue()
    }
}