package org.tobynguyen.kgs.services

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.sqids.Sqids
import org.tobynguyen.kgs.config.AppConfig

@Service
class KeyGenerator(
    private val redisService: RedisService,
    private val appConfig: AppConfig,
    private val sqids: Sqids,
) {
    @Scheduled(fixedRate = 5000)
    fun generateKey() {
        while (redisService.getQueueSize() < appConfig.maxKey) {
            val range = redisService.claimRange(appConfig.batchSize)

            for (n in range) {
                redisService.enqueue(sqids.encode(listOf(n)))
            }
        }
    }
}
