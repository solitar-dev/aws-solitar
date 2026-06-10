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
 */
@DynamoDbImmutable(builder = StatisticsEntity.Builder::class)
class StatisticsEntity private constructor(builder: Builder) {

    @get:DynamoDbPartitionKey val id: String = builder.id ?: GLOBAL_ID

    val totalLinks: Long = builder.totalLinks

    val totalClicks: Long = builder.totalClicks

    @get:DynamoDbConvertedBy(InstantAttributeConverter::class)
    val updatedAt: Instant = builder.updatedAt ?: Instant.now()

    class Builder {
        var id: String? = null
        var totalLinks: Long = 0
        var totalClicks: Long = 0
        var updatedAt: Instant? = null

        fun id(id: String) = apply { this.id = id }

        fun totalLinks(totalLinks: Long) = apply { this.totalLinks = totalLinks }

        fun totalClicks(totalClicks: Long) = apply { this.totalClicks = totalClicks }

        fun updatedAt(updatedAt: Instant) = apply { this.updatedAt = updatedAt }

        fun build() = StatisticsEntity(this)
    }

    companion object {
        const val GLOBAL_ID = "global"

        fun builder() = Builder()
    }
}
