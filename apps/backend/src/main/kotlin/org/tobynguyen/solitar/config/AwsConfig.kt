package org.tobynguyen.solitar.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.tobynguyen.solitar.model.entity.StatisticsEntity
import org.tobynguyen.solitar.model.entity.UrlEntity
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable

/**
 * DynamoDB table beans built on top of the Spring Cloud AWS auto-configured
 * [DynamoDbEnhancedClient]. Region, credentials and endpoint overrides (LocalStack in dev/test) are
 * resolved from `spring.cloud.aws.*` properties. The table schemas are the explicit, native-safe
 * ones in [DynamoSchemas] (NOT `TableSchema.fromImmutableClass`, which fails in the native image).
 */
@Configuration
class AwsConfig {

    @Bean
    fun urlTable(enhancedClient: DynamoDbEnhancedClient): DynamoDbTable<UrlEntity> =
        enhancedClient.table(DynamoTables.URLS, DynamoSchemas.URL)

    @Bean
    fun statisticsTable(enhancedClient: DynamoDbEnhancedClient): DynamoDbTable<StatisticsEntity> =
        enhancedClient.table(DynamoTables.STATISTICS, DynamoSchemas.STATISTICS)
}
