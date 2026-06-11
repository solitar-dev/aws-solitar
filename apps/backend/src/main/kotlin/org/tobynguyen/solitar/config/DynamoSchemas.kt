package org.tobynguyen.solitar.config

import java.time.Instant
import org.tobynguyen.solitar.model.entity.StatisticsEntity
import org.tobynguyen.solitar.model.entity.UrlEntity
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticImmutableTableSchema

/**
 * Explicit DynamoDB table schemas built from compile-time method references, used instead of the
 * reflective [TableSchema.fromImmutableClass].
 *
 * `fromImmutableClass` discovers the entity's getters/setters/constructor by reflection and wires
 * them with `LambdaToMethodBridgeBuilder`, which calls `LambdaMetafactory` to DEFINE lambda classes
 * at runtime. That works on the JVM but a GraalVM native image forbids it ("Classes cannot be
 * defined at runtime") — the app crashes at context refresh while building the `urlTable` bean.
 *
 * Here every getter/setter/builder is a lambda in our own code, so GraalVM resolves them at build
 * time and the native image boots. Keep these in sync with the entity fields.
 */
object DynamoSchemas {

    val URL: TableSchema<UrlEntity> =
        StaticImmutableTableSchema.builder(UrlEntity::class.java, UrlEntity.Builder::class.java)
            .newItemBuilder({ UrlEntity.builder() }, { it.build() })
            .addAttribute(String::class.java) { a ->
                a.name("id").getter { it.id }.setter { b, v -> b.id(v) }.tags(primaryPartitionKey())
            }
            .addAttribute(String::class.java) { a ->
                a.name("originalUrl").getter { it.originalUrl }.setter { b, v -> b.originalUrl(v) }
            }
            .addAttribute(Instant::class.java) { a ->
                a.name("createdAt")
                    .getter { it.createdAt }
                    .setter { b, v -> b.createdAt(v) }
                    .attributeConverter(InstantAttributeConverter())
            }
            .build()

    val STATISTICS: TableSchema<StatisticsEntity> =
        StaticImmutableTableSchema.builder(
                StatisticsEntity::class.java,
                StatisticsEntity.Builder::class.java,
            )
            .newItemBuilder({ StatisticsEntity.builder() }, { it.build() })
            .addAttribute(String::class.java) { a ->
                a.name("id").getter { it.id }.setter { b, v -> b.id(v) }.tags(primaryPartitionKey())
            }
            .addAttribute(Long::class.javaObjectType) { a ->
                a.name("totalLinks").getter { it.totalLinks }.setter { b, v -> b.totalLinks(v) }
            }
            .addAttribute(Long::class.javaObjectType) { a ->
                a.name("totalClicks").getter { it.totalClicks }.setter { b, v -> b.totalClicks(v) }
            }
            .addAttribute(Instant::class.java) { a ->
                a.name("updatedAt")
                    .getter { it.updatedAt }
                    .setter { b, v -> b.updatedAt(v) }
                    .attributeConverter(InstantAttributeConverter())
            }
            .build()
}
