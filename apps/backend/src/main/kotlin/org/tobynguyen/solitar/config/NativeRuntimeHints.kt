package org.tobynguyen.solitar.config

import org.springframework.aot.hint.MemberCategory
import org.springframework.aot.hint.RuntimeHints
import org.springframework.aot.hint.RuntimeHintsRegistrar
import org.tobynguyen.solitar.model.entity.StatisticsEntity
import org.tobynguyen.solitar.model.entity.UrlEntity
import org.tobynguyen.solitar.model.event.LinkCreatedEvent
import org.tobynguyen.solitar.model.event.LinkForwardedEvent

/**
 * GraalVM reachability hints for classes touched reflectively at runtime. Registered via
 * `@ImportRuntimeHints` on the application class.
 *
 * NOTE: The redirect cache is backed by ElastiCache (Valkey) through Spring Data Redis + Lettuce,
 * which ship their own GraalVM reachability metadata in Spring Boot 4 — so no manual Redis/Lettuce
 * hints are registered here. Add one only if a native build flags a specific
 * `ClassNotFoundException` on the Redis path. `registerTypeIfPresent` makes wrong guesses harmless
 * no-ops.
 */
class NativeRuntimeHints : RuntimeHintsRegistrar {

    override fun registerHints(hints: RuntimeHints, classLoader: ClassLoader?) {
        // DynamoDB Enhanced Client immutable schema reflects entity getters + builder setters,
        // plus the explicit Instant converter.
        val enhancedClientTypes =
            listOf(
                UrlEntity::class.java,
                UrlEntity.Builder::class.java,
                StatisticsEntity::class.java,
                StatisticsEntity.Builder::class.java,
                InstantAttributeConverter::class.java,
            )

        // SQS event payloads are (de)serialized by Jackson reflectively.
        val jacksonTypes = listOf(LinkCreatedEvent::class.java, LinkForwardedEvent::class.java)

        (enhancedClientTypes + jacksonTypes).forEach { type ->
            hints
                .reflection()
                .registerType(
                    type,
                    MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                    MemberCategory.INVOKE_DECLARED_METHODS,
                    MemberCategory.DECLARED_FIELDS,
                )
        }

        // SLF4J 2.x resolves its binding via ServiceLoader at startup. In the native image that
        // lookup can be missed, leaving the NOP logger ("No SLF4J providers were found") and no app
        // logs. Register logback's provider class + its service descriptor so logging works
        // natively.
        hints.resources().registerPattern("META-INF/services/org.slf4j.spi.SLF4JServiceProvider")
        hints
            .reflection()
            .registerTypeIfPresent(
                classLoader,
                "ch.qos.logback.classic.spi.LogbackServiceProvider",
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
            )
    }
}
