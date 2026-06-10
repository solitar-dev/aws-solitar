package org.tobynguyen.solitar.service

import io.awspring.cloud.sqs.operations.SqsTemplate
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.tobynguyen.solitar.exception.CodeGenerationException
import org.tobynguyen.solitar.exception.UrlNotFoundException
import org.tobynguyen.solitar.messaging.QueueNames
import org.tobynguyen.solitar.model.dto.UrlCreateDto
import org.tobynguyen.solitar.model.dto.UrlForwardDto
import org.tobynguyen.solitar.model.dto.UrlForwardResponseDto
import org.tobynguyen.solitar.model.entity.UrlEntity
import org.tobynguyen.solitar.model.event.LinkCreatedEvent
import org.tobynguyen.solitar.model.event.LinkForwardedEvent
import org.tobynguyen.solitar.repository.UrlRepository
import org.tobynguyen.solitar.util.CodeGenerator

@Service
class UrlService(
    private val urlRepository: UrlRepository,
    private val sqsTemplate: SqsTemplate,
    private val codeGenerator: CodeGenerator,
) {

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

    /**
     * Mint a short code with a conditional write: generate a random Base62 code (skipping reserved
     * route words) and `putIfAbsent`; on a collision regenerate, up to [MAX_GENERATION_ATTEMPTS].
     * Replaces the deleted KGS key-minting service.
     */
    fun createUrl(data: UrlCreateDto): UrlEntity {
        val (url) = data

        repeat(MAX_GENERATION_ATTEMPTS) {
            val code = codeGenerator.generate()
            if (codeGenerator.isReserved(code)) return@repeat

            val entity = UrlEntity.builder().id(code).originalUrl(url).build()
            if (urlRepository.putIfAbsent(entity)) {
                publish(
                    QueueNames.LINK_CREATED,
                    LinkCreatedEvent(shortCode = code, originalUrl = url),
                )
                return entity
            }
        }

        throw CodeGenerationException()
    }

    /** Best-effort event publish: stats are secondary to the core create/redirect succeeding. */
    private fun publish(queue: String, event: Any) {
        try {
            sqsTemplate.send(queue, event)
        } catch (e: Exception) {
            log.warn("Failed to publish event to '{}' (stats may undercount): {}", queue, e.message)
        }
    }

    private companion object {
        val log = LoggerFactory.getLogger(UrlService::class.java)
        const val MAX_GENERATION_ATTEMPTS = 5
    }
}
