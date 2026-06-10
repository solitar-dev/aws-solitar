package org.tobynguyen.solitar.service

import org.springframework.cache.annotation.Cacheable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.tobynguyen.solitar.exception.UrlNotFoundException
import org.tobynguyen.solitar.mapper.toResponseDto
import org.tobynguyen.solitar.model.dto.UrlCreateDto
import org.tobynguyen.solitar.model.dto.UrlForwardDto
import org.tobynguyen.solitar.model.dto.UrlForwardResponseDto
import org.tobynguyen.solitar.model.entity.UrlEntity
import org.tobynguyen.solitar.model.event.LinkCreatedEvent
import org.tobynguyen.solitar.model.event.LinkForwardedEvent
import org.tobynguyen.solitar.repository.UrlRepository

@Service
class UrlService(
    private val urlRepository: UrlRepository,
    private val kgsRestClient: RestClient,
    private val rabbitMQPublisher: RabbitMQPublisher,
) {

    @Cacheable(value = ["urlForwardResponses"], key = "#data.shortCode")
    fun getOriginalUrl(data: UrlForwardDto): UrlForwardResponseDto {
        val (shortCode) = data

        val urlEntity =
            urlRepository.findByIdOrNull(shortCode)
                ?: throw UrlNotFoundException("Short URL with code '$shortCode' not found.")

        rabbitMQPublisher.publishLinkForwarded(LinkForwardedEvent(shortCode = shortCode))

        return UrlForwardResponseDto(urlEntity.toResponseDto().originalUrl)
    }

    fun createUrl(data: UrlCreateDto): UrlEntity {
        val (url) = data

        val shortCode = kgsRestClient.get().uri("key").retrieve().body<String>()!!

        val entity = urlRepository.save(UrlEntity(id = shortCode, originalUrl = url))

        rabbitMQPublisher.publishLinkCreated(
            LinkCreatedEvent(shortCode = shortCode, originalUrl = url)
        )

        return entity
    }
}
