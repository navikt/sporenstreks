package no.nav.helse.sporenstreks.integrasjon.rest.sts

import io.mockk.every
import io.mockk.mockk
import no.nav.security.token.support.core.jwt.JwtToken
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.text.SimpleDateFormat


class STSClientTest {

    val sdf = SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy")
    val token = mockk<JwtToken>()


    @Test
    fun skal_finne_expired_token() {
        every {
            token.jwtTokenClaims.expirationTime
        } returns sdf.parse("Wed Apr 01 20:58:04 CEST 2020")
        assertThat(isExpired(token, sdf.parse("Wed Apr 01 23:58:04 CEST 2020"))).isTrue()
    }

    @Test
    fun skal_finne_gyldig_token() {
        every {
            token.jwtTokenClaims.expirationTime
        } returns sdf.parse("Wed Apr 01 20:58:04 CEST 2020")
        assertThat(isExpired(token, sdf.parse("Wed Apr 01 20:57:04 CEST 2020"))).isFalse()
    }

}