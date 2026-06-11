package org.tobynguyen.solitar.model.entity

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.tobynguyen.solitar.config.DynamoSchemas

/**
 * Builds the static DynamoDB schemas in isolation — pure JVM, no Docker/LocalStack. Catches
 * schema-mapping mistakes (missing attribute, broken getter/setter, wrong key) in milliseconds.
 *
 * NOTE: this runs on the JVM, so it does NOT catch GraalVM-native-only failures (e.g. the
 * runtime class-definition error from reflective `TableSchema.fromImmutableClass`). The static
 * schemas in [DynamoSchemas] exist precisely to keep the native image booting; that gate is the
 * native build + boot.
 */
class DynamoSchemaTest {

    @Test
    fun `url schema builds with id as partition key`() {
        assertEquals("id", DynamoSchemas.URL.tableMetadata().primaryPartitionKey())
    }

    @Test
    fun `statistics schema builds with id as partition key`() {
        assertEquals("id", DynamoSchemas.STATISTICS.tableMetadata().primaryPartitionKey())
    }
}
