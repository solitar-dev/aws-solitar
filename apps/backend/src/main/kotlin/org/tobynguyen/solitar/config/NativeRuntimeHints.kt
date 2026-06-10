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
 * NOTE: The Caffeine class list below is a best-effort starting set covering a bounded +
 * expireAfterWrite cache. The EXACT set must be confirmed by running the native spike
 * (`nativeCompile` on a Docker/ARM64 host — phase-02 step 1) and adding any class the build/runtime
 * flags as `ClassNotFoundException`. `registerTypeIfPresent` makes wrong guesses harmless no-ops.
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

        // Caffeine generates its cache implementation + factory classes by feature combination and
        // loads them by name. Confirm/extend against the native spike output.
        listOf(
                "com.github.benmanes.caffeine.cache.SSMSW",
                "com.github.benmanes.caffeine.cache.SSLMSW",
                "com.github.benmanes.caffeine.cache.SSMSA",
                "com.github.benmanes.caffeine.cache.PSMS",
                "com.github.benmanes.caffeine.cache.PSWMS",
                "com.github.benmanes.caffeine.cache.BoundedLocalCache",
            )
            .forEach { name ->
                hints
                    .reflection()
                    .registerTypeIfPresent(
                        classLoader,
                        name,
                        MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                    )
            }
    }
}
