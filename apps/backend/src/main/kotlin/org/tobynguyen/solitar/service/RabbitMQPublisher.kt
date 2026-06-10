package org.tobynguyen.solitar.service

import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import org.tobynguyen.solitar.config.RabbitMQConfig
import org.tobynguyen.solitar.model.event.LinkCreatedEvent
import org.tobynguyen.solitar.model.event.LinkForwardedEvent

@Service
class RabbitMQPublisher(private val rabbitTemplate: RabbitTemplate) {
    fun publishLinkCreated(event: LinkCreatedEvent) {
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.EXCHANGE_NAME,
            RabbitMQConfig.LINK_CREATED_ROUTING_KEY,
            event,
        )
    }

    fun publishLinkForwarded(event: LinkForwardedEvent) {
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.EXCHANGE_NAME,
            RabbitMQConfig.LINK_FORWARDED_ROUTING_KEY,
            event,
        )
    }
}
