package org.tobynguyen.solitar.config

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.data.redis.serializer.StringRedisSerializer

/**
 * Proves the redirect cache value (a plain target-URL String) round-trips through the Valkey value
 * serializer with no Docker and no Spring context — pure JVM. Guards the decision to cache a String
 * (not the immutable, builder-only `UrlEntity`) against a regression to a serializer that can't
 * round-trip it. The configured key/value serializer in [CacheConfig] is exactly this one.
 */
class CacheSerializationTest {

    private val serializer = StringRedisSerializer()

    @Test
    fun `target url round-trips through the valkey value serializer`() {
        val url = "https://example.com/some/very/long/path?q=1&x=2#frag"

        val restored = serializer.deserialize(serializer.serialize(url))

        assertEquals(url, restored)
    }

    @Test
    fun `null deserializes to null`() {
        // `@Cacheable(unless = "#result == null")` never stores nulls, but the serializer must still
        // tolerate an absent value defensively.
        assertEquals(null, serializer.deserialize(null))
    }
}
