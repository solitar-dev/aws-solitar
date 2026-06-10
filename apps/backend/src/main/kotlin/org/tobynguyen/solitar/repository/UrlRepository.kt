package org.tobynguyen.solitar.repository

import org.springframework.data.cassandra.repository.CassandraRepository
import org.tobynguyen.solitar.model.entity.UrlEntity

interface UrlRepository : CassandraRepository<UrlEntity, String> {}
