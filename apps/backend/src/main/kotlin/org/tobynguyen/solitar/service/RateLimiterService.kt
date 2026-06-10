package org.tobynguyen.solitar.service

import io.github.bucket4j.Bucket
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.ConsumptionProbe
import java.util.concurrent.ConcurrentHashMap
import org.springframework.stereotype.Service

/**
 * In-memory token-bucket rate limiter (replaces the Redis-backed Lettuce proxy manager). One
 * [Bucket] per key, held in a [ConcurrentHashMap]. Single-task scoped — see [RateLimiterConfig].
 */
@Service
class RateLimiterService(private val bucketConfiguration: BucketConfiguration) {

    private val buckets = ConcurrentHashMap<String, Bucket>()

    fun tryConsumeAndReturnRemaining(key: String): ConsumptionProbe =
        buckets.computeIfAbsent(key) { newBucket() }.tryConsumeAndReturnRemaining(1)

    private fun newBucket(): Bucket {
        val builder = Bucket.builder()
        bucketConfiguration.bandwidths.forEach { builder.addLimit(it) }
        return builder.build()
    }
}
