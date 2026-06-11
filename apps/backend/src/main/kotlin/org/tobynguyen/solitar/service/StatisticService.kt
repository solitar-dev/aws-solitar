package org.tobynguyen.solitar.service

import org.springframework.stereotype.Service
import org.tobynguyen.solitar.model.dto.StatisticDto
import org.tobynguyen.solitar.repository.StatisticsRepository

@Service
class StatisticService(private val statisticsRepository: StatisticsRepository) {

    fun getStatistic(): StatisticDto {
        val stats = statisticsRepository.findGlobal()
        return StatisticDto(
            totalLinks = stats?.totalLinks ?: 0,
            totalClicks = stats?.totalClicks ?: 0,
        )
    }
}
