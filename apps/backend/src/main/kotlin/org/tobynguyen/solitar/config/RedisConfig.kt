package org.tobynguyen.solitar.config

import java.time.Duration
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.tobynguyen.solitar.model.dto.UrlForwardResponseDto

@Configuration
@EnableCaching
class RedisConfig {
    @Bean
    fun cacheManager(connectionFactory: RedisConnectionFactory): CacheManager {
        val urlForwardResponseSerializer =
            JacksonJsonRedisSerializer(UrlForwardResponseDto::class.java)

        val urlForwardResponseConfig =
            RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(24))
                .disableCachingNullValues()
                .serializeValuesWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(
                        urlForwardResponseSerializer
                    )
                )

        val config = mapOf("urlForwardResponses" to urlForwardResponseConfig)

        return RedisCacheManager.builder(connectionFactory)
            .withInitialCacheConfigurations(config)
            .build()
    }
}
