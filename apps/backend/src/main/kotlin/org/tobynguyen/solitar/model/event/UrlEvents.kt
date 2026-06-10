package org.tobynguyen.solitar.model.event

import java.time.Instant

data class LinkCreatedEvent(
    val shortCode: String,
    val originalUrl: String,
    val createdAt: Instant = Instant.now(),
)

data class LinkForwardedEvent(val shortCode: String, val forwardedAt: Instant = Instant.now())
