package org.tobynguyen.solitar.config

import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitMQConfig {

    companion object {
        const val EXCHANGE_NAME = "solitar.events"

        const val LINK_CREATED_QUEUE = "link.created"
        const val LINK_FORWARDED_QUEUE = "link.forwarded"

        const val LINK_CREATED_ROUTING_KEY = "link.created"
        const val LINK_FORWARDED_ROUTING_KEY = "link.forwarded"
    }

    @Bean fun solitarExchange(): DirectExchange = DirectExchange(EXCHANGE_NAME)

    @Bean fun linkCreatedQueue(): Queue = Queue(LINK_CREATED_QUEUE, true)

    @Bean fun linkForwardedQueue(): Queue = Queue(LINK_FORWARDED_QUEUE, true)

    @Bean
    fun linkCreatedBinding(linkCreatedQueue: Queue, solitarExchange: DirectExchange): Binding =
        BindingBuilder.bind(linkCreatedQueue).to(solitarExchange).with(LINK_CREATED_ROUTING_KEY)

    @Bean
    fun linkForwardedBinding(linkForwardedQueue: Queue, solitarExchange: DirectExchange): Binding =
        BindingBuilder.bind(linkForwardedQueue).to(solitarExchange).with(LINK_FORWARDED_ROUTING_KEY)

    @Bean fun jsonMessageConverter(): JacksonJsonMessageConverter = JacksonJsonMessageConverter()
}
