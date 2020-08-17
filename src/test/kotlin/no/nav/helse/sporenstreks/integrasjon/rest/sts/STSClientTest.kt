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
        assertThat(true).isTrue()
    }

    @Test
    fun skal_finne_gyldig_token() {
        assertThat(false).isFalse()
    }

}