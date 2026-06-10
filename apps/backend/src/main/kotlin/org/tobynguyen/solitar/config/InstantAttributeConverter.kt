package org.tobynguyen.solitar.config

import java.time.Instant
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

/**
 * Explicit ISO-8601 string converter for [Instant]. The Enhanced Client ships a default Instant
 * converter, but it is resolved reflectively; declaring an explicit converter keeps the mapping
 * GraalVM-native-safe (reflection hints land in P2).
 */
class InstantAttributeConverter : AttributeConverter<Instant> {
    override fun transformFrom(input: Instant): AttributeValue =
        AttributeValue.builder().s(input.toString()).build()

    override fun transformTo(input: AttributeValue): Instant = Instant.parse(input.s())

    override fun type(): EnhancedType<Instant> = EnhancedType.of(Instant::class.java)

    override fun attributeValueType(): AttributeValueType = AttributeValueType.S
}
