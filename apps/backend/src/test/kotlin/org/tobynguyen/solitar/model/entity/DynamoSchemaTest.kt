package org.tobynguyen.solitar.model.entity

import org.junit.jupiter.api.Test
import software.amazon.awssdk.enhanced.dynamodb.TableSchema

/**
 * Builds the Enhanced Client immutable schemas in isolation — pure JVM, no Docker/Spring/LocalStack.
 * Guards the builder-introspection trap where a Kotlin `var` exposes BOTH a generated `setX` and a
 * fluent `x(...)` setter, which makes `fromImmutableClass` fail at runtime with
 * "Duplicate key ...". This is the millisecond check that catches the context-refresh crash the
 * full SpringBootTest only surfaces with a Docker daemon.
 */
class DynamoSchemaTest {

    @Test
    fun `url entity immutable schema builds`() {
        TableSchema.fromImmutableClass(UrlEntity::class.java)
    }

    @Test
    fun `statistics entity immutable schema builds`() {
        TableSchema.fromImmutableClass(StatisticsEntity::class.java)
    }
}
