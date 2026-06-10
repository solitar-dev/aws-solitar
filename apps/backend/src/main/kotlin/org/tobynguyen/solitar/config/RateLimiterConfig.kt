package org.tobynguyen.solitar.config

import io.github.bucket4j.BucketConfiguration
import java.time.Duration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.tobynguyen.solitar.config.properties.AppConfig

/**
 * In-memory rate-limit configuration (replaces the Redis/Lettuce proxy manager). Buckets are held
 * per-key in [org.tobynguyen.solitar.service.RateLimiterService]; this is single-task scoped — a
 * restart or a second task resets/splits limits (accepted for the single-Fargate-task design).
 */
@Configuration
class RateLimiterConfig(private val appConfig: AppConfig) {

    @Bean
    fun bucketConfiguration(): BucketConfiguration =
        BucketConfiguration.builder()
            .addLimit {
                it.capacity(appConfig.rateLimiter.capacity)
                    .refillIntervally(
                        appConfig.rateLimiter.capacity,
                        Duration.ofSeconds(appConfig.rateLimiter.refillPeriodInSeconds),
                    )
            }
            .build()
}
