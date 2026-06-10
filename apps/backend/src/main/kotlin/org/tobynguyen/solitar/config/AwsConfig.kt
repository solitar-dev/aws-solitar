package org.tobynguyen.solitar.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.tobynguyen.solitar.model.entity.StatisticsEntity
import org.tobynguyen.solitar.model.entity.UrlEntity
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable
import software.amazon.awssdk.enhanced.dynamodb.TableSchema

/**
 * DynamoDB table beans built on top of the Spring Cloud AWS auto-configured
 * [DynamoDbEnhancedClient]. Region, credentials and endpoint overrides (LocalStack in dev/test) are
 * resolved from `spring.cloud.aws.*` properties.
 */
@Configuration
class AwsConfig {

    @Bean
    fun urlTable(enhancedClient: DynamoDbEnhancedClient): DynamoDbTable<UrlEntity> =
        enhancedClient.table(
            DynamoTables.URLS,
            TableSchema.fromImmutableClass(UrlEntity::class.java),
        )

    @Bean
    fun statisticsTable(enhancedClient: DynamoDbEnhancedClient): DynamoDbTable<StatisticsEntity> =
        enhancedClient.table(
            DynamoTables.STATISTICS,
            TableSchema.fromImmutableClass(StatisticsEntity::class.java),
        )
}
