package org.tobynguyen.solitar.service

import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.stereotype.Service
import org.tobynguyen.solitar.messaging.QueueNames
import org.tobynguyen.solitar.model.event.LinkCreatedEvent
import org.tobynguyen.solitar.model.event.LinkForwardedEvent
import org.tobynguyen.solitar.repository.StatisticsRepository

/**
 * Consumes link events from SQS and applies atomic counter increments. SQS is at-least-once; the
 * `UpdateItem ADD` increments are commutative so redelivery is safe (may marginally over-count).
 */
@Service
class StatisticsAggregator(private val statisticsRepository: StatisticsRepository) {

    @SqsListener(QueueNames.LINK_CREATED)
    fun onLinkCreated(event: LinkCreatedEvent) {
        statisticsRepository.incrementLinks()
    }

    @SqsListener(QueueNames.LINK_FORWARDED)
    fun onLinkForwarded(event: LinkForwardedEvent) {
        statisticsRepository.incrementClicks()
    }
}
