package org.tobynguyen.solitar.service

import io.awspring.cloud.sqs.operations.SqsTemplate
import java.util.concurrent.ThreadLocalRandom
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.tobynguyen.solitar.exception.ShortCodeConflictException
import org.tobynguyen.solitar.exception.UrlNotFoundException
import org.tobynguyen.solitar.messaging.QueueNames
import org.tobynguyen.solitar.model.dto.UrlCreateDto
import org.tobynguyen.solitar.model.dto.UrlForwardDto
import org.tobynguyen.solitar.model.dto.UrlForwardResponseDto
import org.tobynguyen.solitar.model.entity.UrlEntity
import org.tobynguyen.solitar.model.event.LinkCreatedEvent
import org.tobynguyen.solitar.model.event.LinkForwardedEvent
import org.tobynguyen.solitar.repository.UrlRepository

@Service
class UrlService(private val urlRepository: UrlRepository, private val sqsTemplate: SqsTemplate) {

    /**
     * Resolve a short code to its target URL. The lookup is Caffeine-cached (in the repository),
     * but a [LinkForwardedEvent] is published on EVERY call so click stats stay exact. Publishing
     * is best-effort: a transient SQS failure drops the click, never the redirect.
     */
    fun resolve(shortCode: String): UrlEntity {
        val entity =
            urlRepository.findById(shortCode)
                ?: throw UrlNotFoundException("Short URL with code '$shortCode' not found.")

        publish(QueueNames.LINK_FORWARDED, LinkForwardedEvent(shortCode = shortCode))

        return entity
    }

    /**
     * Deprecated JSON forward API (frontend legacy). Shares [resolve]; superseded by `GET /{code}`,
     * retire in P5/P6 once nothing calls `POST /forward`.
     */
    fun getOriginalUrl(data: UrlForwardDto): UrlForwardResponseDto =
        UrlForwardResponseDto(resolve(data.shortCode).originalUrl)

    fun createUrl(data: UrlCreateDto): UrlEntity {
        val (url) = data

        val shortCode = generateCode()
        val entity = UrlEntity.builder().id(shortCode).originalUrl(url).build()

        // TODO(P2): regenerate + retry on collision instead of surfacing a 409 to the caller.
        if (!urlRepository.putIfAbsent(entity)) {
            throw ShortCodeConflictException(
                "Short code '$shortCode' already exists; please retry."
            )
        }

        publish(QueueNames.LINK_CREATED, LinkCreatedEvent(shortCode = shortCode, originalUrl = url))

        return entity
    }

    /** Best-effort event publish: stats are secondary to the core create/redirect succeeding. */
    private fun publish(queue: String, event: Any) {
        try {
            sqsTemplate.send(queue, event)
        } catch (e: Exception) {
            log.warn("Failed to publish event to '{}' (stats may undercount): {}", queue, e.message)
        }
    }

    /**
     * Single code-generation seam (interim random Base62). P2 replaces with the KGS-free strategy.
     */
    private fun generateCode(): String {
        val random = ThreadLocalRandom.current()
        return buildString {
            repeat(CODE_LENGTH) { append(ALPHABET[random.nextInt(ALPHABET.length)]) }
        }
    }

    private companion object {
        val log = LoggerFactory.getLogger(UrlService::class.java)
        const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        const val CODE_LENGTH = 7
    }
}
