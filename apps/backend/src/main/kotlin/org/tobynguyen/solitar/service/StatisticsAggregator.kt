package org.tobynguyen.solitar.service

import java.time.Instant
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.data.repository.findByIdOrNull
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.tobynguyen.solitar.config.RabbitMQConfig
import org.tobynguyen.solitar.model.entity.StatisticsEntity
import org.tobynguyen.solitar.repository.StatisticsRepository

@Service
class StatisticsAggregator(
    private val rabbitTemplate: RabbitTemplate,
    private val statisticsRepository: StatisticsRepository,
) {
    @Scheduled(fixedRate = 5 * 60 * 1000)
    fun aggregate() {
        val createdCount = drainQueue(RabbitMQConfig.LINK_CREATED_QUEUE)
        val forwardedCount = drainQueue(RabbitMQConfig.LINK_FORWARDED_QUEUE)

        if (createdCount == 0L && forwardedCount == 0L) {
            return
        }

        val current =
            statisticsRepository.findByIdOrNull("global") ?: StatisticsEntity(id = "global")

        val updated =
            current.copy(
                totalLinks = current.totalLinks + createdCount,
                totalClicks = current.totalClicks + forwardedCount,
                updatedAt = Instant.now(),
            )

        statisticsRepository.save(updated)
    }

    private fun drainQueue(queueName: String): Long =
        generateSequence { rabbitTemplate.receive(queueName) }.count().toLong()
}
