package org.tobynguyen.solitar.model.entity

import java.time.Instant
import org.springframework.data.cassandra.core.mapping.PrimaryKey
import org.springframework.data.cassandra.core.mapping.Table

@Table("statistics")
data class StatisticsEntity(
    @PrimaryKey val id: String = "global",
    var totalLinks: Long = 0,
    var totalClicks: Long = 0,
    var updatedAt: Instant = Instant.now(),
)
