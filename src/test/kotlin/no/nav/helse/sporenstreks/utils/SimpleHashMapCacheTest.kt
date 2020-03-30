package no.nav.helse.sporenstreks.utils

import io.mockk.every
import io.mockk.mockk
import no.nav.helse.sporenstreks.system.TimeProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.BeforeEach
import java.time.Duration
import java.time.LocalDateTime

internal class SimpleHashMapCacheTest {
    private val timeProviderMock = mockk<TimeProvider>()
    private val duration = Duration.ofMinutes(10)
    private val maxCacheSize = 1

    val cache = SimpleHashMapCache<String>(duration, maxCacheSize, timeProviderMock)

    val key = "123"
    val value = "cachedValue"
    
    private val startTime = LocalDateTime.of(2010, 1, 10, 1,1)

    @BeforeEach
    fun setup() {
        every {timeProviderMock.now() } returns startTime
    }

    @Test
    fun `Cache expires properly`() {
        assertThat(cache.hasValidCacheEntry(key)).isFalse()

        cache.put(key, value)
        assertThat(cache.hasValidCacheEntry(key)).isTrue()
        assertThat(cache.get(key)).isEqualTo(value)

        // cache expires
        every {timeProviderMock.now() } returns startTime.plusMinutes(duration.toMinutes()).plusSeconds(1)
        assertThat(cache.hasValidCacheEntry(key)).isFalse()
    }

    @Test
    fun `Expired items are evicted when maxSize is reached`() {
        cache.put(key, value)
        assertThat(cache.size).isEqualTo(maxCacheSize)

        every {timeProviderMock.now() } returns startTime.plusMinutes(duration.toMinutes()).plusSeconds(1)
        cache.put("newKey", "newValue")

        assertThat(cache.size).isEqualTo(maxCacheSize)
        assertThat(cache.hasValidCacheEntry("newKey")).isTrue()
        assertThat(cache.hasValidCacheEntry(key)).isFalse()
    }

    @Test
    fun `When cache is full of valid items, new items are not cached`() {
        cache.put(key, value)
        assertThat(cache.size).isEqualTo(maxCacheSize)

        cache.put("newKey", "newValue")

        assertThat(cache.size).isEqualTo(maxCacheSize)
        assertThat(cache.hasValidCacheEntry(key)).isTrue()
        assertThat(cache.hasValidCacheEntry("newKey")).isFalse()
    }
}