package org.tobynguyen.solitar.config

import com.github.benmanes.caffeine.cache.Caffeine
import java.time.Duration
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * In-memory Caffeine cache (replaces Redis). Caches the DynamoDB URL lookup only — the redirect
 * path still publishes a forwarded event on every hit so click stats stay exact.
 */
@Configuration
@EnableCaching
class CacheConfig {

    companion object {
        const val URL_FORWARD_CACHE = "urlForwardResponses"
    }

    @Bean
    fun cacheManager(): CacheManager {
        val manager = CaffeineCacheManager(URL_FORWARD_CACHE)
        manager.setCaffeine(
            Caffeine.newBuilder().expireAfterWrite(Duration.ofHours(24)).maximumSize(10_000)
        )
        return manager
    }
}
