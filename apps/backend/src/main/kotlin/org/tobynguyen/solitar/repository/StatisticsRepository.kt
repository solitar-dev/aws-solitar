package org.tobynguyen.solitar.repository

import java.time.Instant
import org.springframework.stereotype.Repository
import org.tobynguyen.solitar.config.DynamoTables
import org.tobynguyen.solitar.model.entity.StatisticsEntity
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

/**
 * Global statistics row. Reads go through the Enhanced Client; increments use a low-level atomic
 * `UpdateItem ADD` so concurrent redirects/creates never lose a count (commutative, idempotent by
 * construction — at-least-once SQS delivery may over-count marginally; acceptable for hobby stats).
 */
@Repository
class StatisticsRepository(
    private val statisticsTable: DynamoDbTable<StatisticsEntity>,
    private val dynamoDbClient: DynamoDbClient,
) {

    fun findGlobal(): StatisticsEntity? =
        statisticsTable.getItem(Key.builder().partitionValue(StatisticsEntity.GLOBAL_ID).build())

    fun incrementLinks() = addToCounter("totalLinks")

    fun incrementClicks() = addToCounter("totalClicks")

    private fun addToCounter(attribute: String) {
        dynamoDbClient.updateItem { req ->
            req.tableName(DynamoTables.STATISTICS)
                .key(mapOf("id" to AttributeValue.fromS(StatisticsEntity.GLOBAL_ID)))
                .updateExpression("ADD #counter :one SET updatedAt = :now")
                .expressionAttributeNames(mapOf("#counter" to attribute))
                .expressionAttributeValues(
                    mapOf(
                        ":one" to AttributeValue.fromN("1"),
                        ":now" to AttributeValue.fromS(Instant.now().toString()),
                    )
                )
        }
    }
}
