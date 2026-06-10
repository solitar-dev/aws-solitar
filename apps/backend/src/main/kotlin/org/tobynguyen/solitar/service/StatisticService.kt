package org.tobynguyen.solitar.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.tobynguyen.solitar.model.dto.StatisticDto
import org.tobynguyen.solitar.model.entity.StatisticsEntity
import org.tobynguyen.solitar.repository.StatisticsRepository

@Service
class StatisticService(private val statisticsRepository: StatisticsRepository) {

    fun getStatistic(): StatisticDto {
        val stats: StatisticsEntity =
            statisticsRepository.findByIdOrNull("global") ?: StatisticsEntity()
        return StatisticDto(totalLinks = stats.totalLinks, totalClicks = stats.totalClicks)
    }
}
