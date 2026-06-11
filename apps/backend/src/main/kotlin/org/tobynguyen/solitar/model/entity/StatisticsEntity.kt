package org.tobynguyen.solitar.model.entity

import java.time.Instant
import org.tobynguyen.solitar.config.InstantAttributeConverter
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbConvertedBy
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey

/**
 * DynamoDB `statistics` item — a single row keyed `id="global"`. Counters are mutated atomically
 * via `UpdateItem ADD` in [org.tobynguyen.solitar.repository.StatisticsRepository]; this entity is
 * used only for reads.
 *
 * Builder uses PRIVATE fields + fluent setters only (no public `getX`/`setX`) — see
 * [UrlEntity] for why the immutable introspector requires that shape.
 */
@DynamoDbImmutable(builder = StatisticsEntity.Builder::class)
class StatisticsEntity
private constructor(
    @get:DynamoDbPartitionKey val id: String,
    val totalLinks: Long,
    val totalClicks: Long,
    @get:DynamoDbConvertedBy(InstantAttributeConverter::class) val updatedAt: Instant,
) {

    class Builder {
        private var id: String? = null
        private var totalLinks: Long = 0
        private var totalClicks: Long = 0
        private var updatedAt: Instant? = null

        fun id(id: String) = apply { this.id = id }

        fun totalLinks(totalLinks: Long) = apply { this.totalLinks = totalLinks }

        fun totalClicks(totalClicks: Long) = apply { this.totalClicks = totalClicks }

        fun updatedAt(updatedAt: Instant) = apply { this.updatedAt = updatedAt }

        fun build() =
            StatisticsEntity(
                id = id ?: GLOBAL_ID,
                totalLinks = totalLinks,
                totalClicks = totalClicks,
                updatedAt = updatedAt ?: Instant.now(),
            )
    }

    companion object {
        const val GLOBAL_ID = "global"

        fun builder() = Builder()
    }
}
