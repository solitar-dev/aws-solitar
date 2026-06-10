package org.tobynguyen.solitar.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app")
data class AppConfig(
    val cors: CorsConfig = CorsConfig(),
    val rateLimiter: RateLimiterConfig = RateLimiterConfig(),
    val kgsEndpoint: String = "http://localhost:8081",
) {
    data class CorsConfig(val allowedOrigins: List<String> = emptyList())

    data class RateLimiterConfig(val capacity: Long = 10L, val refillPeriodInSeconds: Long = 3600L)
}
