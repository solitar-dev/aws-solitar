package org.tobynguyen.solitar.model.entity

import java.time.Instant
import org.tobynguyen.solitar.config.InstantAttributeConverter
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbConvertedBy
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey

/**
 * DynamoDB `urls` item. Uses the Enhanced Client `@DynamoDbImmutable` + builder style so the class
 * stays immutable and avoids the no-arg-ctor/mutable-field requirement of `@DynamoDbBean`.
 *
 * The builder backs its fields with PRIVATE fields and exposes only fluent setters + `build()` — it
 * must NOT expose public `getX`/`setX` accessors. A Kotlin `var` on the builder would generate both
 * a `setId(...)` (colliding with the fluent `id(...)` → "Duplicate key") and a `getId()` (which the
 * immutable introspector rejects as an unmatched builder method). `build()` validates and passes the
 * values to the private constructor.
 */
@DynamoDbImmutable(builder = UrlEntity.Builder::class)
class UrlEntity
private constructor(
    @get:DynamoDbPartitionKey val id: String,
    val originalUrl: String,
    @get:DynamoDbConvertedBy(InstantAttributeConverter::class) val createdAt: Instant,
) {

    class Builder {
        private var id: String? = null
        private var originalUrl: String? = null
        private var createdAt: Instant? = null

        fun id(id: String) = apply { this.id = id }

        fun originalUrl(originalUrl: String) = apply { this.originalUrl = originalUrl }

        fun createdAt(createdAt: Instant) = apply { this.createdAt = createdAt }

        fun build() =
            UrlEntity(
                id = requireNotNull(id) { "id is required" },
                originalUrl = requireNotNull(originalUrl) { "originalUrl is required" },
                createdAt = createdAt ?: Instant.now(),
            )
    }

    companion object {
        fun builder() = Builder()
    }
}
