package org.tobynguyen.kgs.services

import jakarta.annotation.PostConstruct
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import org.tobynguyen.kgs.config.AppConfig

@Service
class RedisService(private val redisTemplate: StringRedisTemplate, private val appConfig: AppConfig) {

    companion object {
        const val QUEUE_NAME = "kgs_queue"
        const val COUNTER_NAME = "kgs_counter"
    }

    @PostConstruct
    fun initCounter() {
        redisTemplate.opsForValue().setIfAbsent(COUNTER_NAME, appConfig.counterStart.toString())
    }

    fun enqueue(message: String) {
        redisTemplate.opsForList().leftPush(QUEUE_NAME, message)
    }

    fun dequeue(): String? {
        return redisTemplate.opsForList().rightPop(QUEUE_NAME)
    }

    fun getQueueSize(): Long {
        val size = redisTemplate.opsForList().size(QUEUE_NAME)

        return size ?: 0L
    }

    fun claimRange(batchSize: Long): LongRange {
        val end = redisTemplate.opsForValue().increment(COUNTER_NAME, batchSize)

        return (end - batchSize + 1)..end
    }
}
