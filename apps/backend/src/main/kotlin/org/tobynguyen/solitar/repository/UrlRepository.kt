package org.tobynguyen.solitar.repository

import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Repository
import org.tobynguyen.solitar.config.CacheConfig
import org.tobynguyen.solitar.model.entity.UrlEntity
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable
import software.amazon.awssdk.enhanced.dynamodb.Expression
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException

/**
 * DynamoDB-backed URL store (Enhanced Client). The redirect lookup is cached in ElastiCache
 * (Valkey) as a plain `code -> originalUrl` String; the redirect path still emits a forwarded event
 * per hit (see [org.tobynguyen.solitar.service.UrlService]).
 */
@Repository
class UrlRepository(private val urlTable: DynamoDbTable<UrlEntity>) {

    /**
     * Resolve a code to its target URL, cached in Valkey. Returns the `originalUrl` String (not the
     * immutable [UrlEntity]) so the cache value is a trivial String round-trip. Called cross-bean
     * from [org.tobynguyen.solitar.service.UrlService] so the `@Cacheable` proxy actually engages.
     */
    @Cacheable(value = [CacheConfig.URL_FORWARD_CACHE], key = "#code", unless = "#result == null")
    fun findOriginalUrl(code: String): String? =
        urlTable.getItem(Key.builder().partitionValue(code).build())?.originalUrl

    /**
     * Conditional put guaranteeing the code is unused. Returns `false` on collision so the caller
     * can regenerate. P2 wraps this in a bounded retry + reserved-word filter.
     */
    fun putIfAbsent(entity: UrlEntity): Boolean =
        try {
            urlTable.putItem(
                PutItemEnhancedRequest.builder(UrlEntity::class.java)
                    .item(entity)
                    .conditionExpression(
                        Expression.builder().expression("attribute_not_exists(id)").build()
                    )
                    .build()
            )
            true
        } catch (_: ConditionalCheckFailedException) {
            false
        }
}
