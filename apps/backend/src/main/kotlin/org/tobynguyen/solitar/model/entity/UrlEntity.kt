package org.tobynguyen.solitar.model.entity

import java.time.Instant
import org.tobynguyen.solitar.config.InstantAttributeConverter
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbConvertedBy
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey

/**
 * DynamoDB `urls` item. Uses the Enhanced Client `@DynamoDbImmutable` + builder style so the class
 * stays immutable and avoids the no-arg-ctor/mutable-field requirement of `@DynamoDbBean`. The
 * builder is the only native-image surface (final schema validated in P2 spike).
 */
@DynamoDbImmutable(builder = UrlEntity.Builder::class)
class UrlEntity private constructor(builder: Builder) {

    @get:DynamoDbPartitionKey val id: String = requireNotNull(builder.id) { "id is required" }

    val originalUrl: String = requireNotNull(builder.originalUrl) { "originalUrl is required" }

    @get:DynamoDbConvertedBy(InstantAttributeConverter::class)
    val createdAt: Instant = builder.createdAt ?: Instant.now()

    class Builder {
        var id: String? = null
        var originalUrl: String? = null
        var createdAt: Instant? = null

        fun id(id: String) = apply { this.id = id }

        fun originalUrl(originalUrl: String) = apply { this.originalUrl = originalUrl }

        fun createdAt(createdAt: Instant) = apply { this.createdAt = createdAt }

        fun build() = UrlEntity(this)
    }

    companion object {
        fun builder() = Builder()
    }
}
