package no.nav.helse.sporenstreks.utils

import java.time.Duration
import java.time.LocalDateTime


class SimpleHashMapCache<T>(private val cacheDuration: Duration, private val maxCachedItems: Int) {
    private val cache = mutableMapOf<String, Pair<LocalDateTime, T>>()

    fun hasValidCacheEntry(key: String): Boolean {
        return cache[key]?.first?.isAfter(LocalDateTime.now()) ?: false
    }

    fun get(key: String): T {
        return cache[key]!!.second
    }

    fun put(key: String, value: T) {
        if (cache.keys.size >= maxCachedItems) {
            cache.filterValues { it.first.isAfter(LocalDateTime.now()) }.keys
                    .forEach { cache.remove(it) }
        }

        if (cache.keys.size < maxCachedItems) {
            cache[key] = Pair(LocalDateTime.now().plusMinutes(cacheDuration.toMinutes()), value)
        }
    }
}

