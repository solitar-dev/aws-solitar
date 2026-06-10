package org.tobynguyen.solitar.repository

import org.springframework.data.cassandra.repository.CassandraRepository
import org.tobynguyen.solitar.model.entity.StatisticsEntity

interface StatisticsRepository : CassandraRepository<StatisticsEntity, String>
