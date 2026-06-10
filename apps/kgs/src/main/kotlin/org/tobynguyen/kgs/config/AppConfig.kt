package org.tobynguyen.kgs.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app")
data class AppConfig(val maxKey: Long = 100L, val batchSize: Long = 10L, val counterStart: Long = 0L) {}
