package org.tobynguyen.solitar.config

import java.time.Duration
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer

/**
 * ElastiCache (Valkey) cache via Spring Data Redis (Lettuce). Only the resolved target URL is
 * cached (code -> originalUrl, a plain String), so both key and value go through a trivial,
 * native-image-safe [StringRedisSerializer] — the immutable, builder-only `UrlEntity` is never
 * serialized. The 24h TTL mirrors the previous in-process cache's write-expiry. The redirect path
 * still publishes a forwarded event on every hit so click stats stay exact.
 *
 * The [RedisConnectionFactory] is built by Spring Boot's Redis auto-configuration from
 * `spring.data.redis.*` (host/port/ssl), which is env-driven: loopback + no TLS locally, the
 * serverless endpoint + TLS in prod (wired by the ECS task env).
 */
@Configuration
@EnableCaching
class CacheConfig {

    companion object {
        const val URL_FORWARD_CACHE = "urlForwardResponses"
    }

    @Bean
    fun cacheManager(connectionFactory: RedisConnectionFactory): CacheManager {
        val stringPair =
            RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer())
        val cacheDefaults =
            RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(24))
                .serializeKeysWith(stringPair)
                .serializeValuesWith(stringPair)
        // Lock the manager to the one known cache: a typo'd cache name fails loudly instead of
        // silently creating a stray Valkey keyspace.
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(cacheDefaults)
            .initialCacheNames(setOf(URL_FORWARD_CACHE))
            .disableCreateOnMissingCache()
            .build()
    }
}
