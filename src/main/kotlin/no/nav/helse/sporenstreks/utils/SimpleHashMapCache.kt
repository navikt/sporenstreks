package no.nav.helse.sporenstreks.utils

import no.nav.helse.sporenstreks.system.TimeProvider
import java.time.Duration
import java.time.LocalDateTime


class SimpleHashMapCache<T>(
        private val cacheDuration: Duration,
        private val maxCachedItems: Int,
        private val timeProvider: TimeProvider = object: TimeProvider {}
) {
    private val cache = mutableMapOf<String, Entry<T>>()
    val size: Int
        get() {return cache.size}

    fun hasValidCacheEntry(key: String): Boolean {
        return cache[key]?.isValid() ?: false
    }

    fun get(key: String): T {
        return cache[key]!!.value
    }

    fun put(key: String, value: T) {
        if (cache.keys.size >= maxCachedItems) {
            cache.filterValues { !it.isValid() }.keys
                    .forEach { cache.remove(it) }
        }

        if (cache.keys.size < maxCachedItems) {
            cache[key] = Entry(timeProvider.now().plus(cacheDuration), value)
        }
    }

    fun clearCache() {
        cache.clear()
    }

    private data class Entry<T>(val expiryTime: LocalDateTime, val value: T)

    private fun Entry<T>.isValid() = this.expiryTime.isAfter(timeProvider.now())
}

