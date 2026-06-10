package org.tobynguyen.solitar.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class RestClientConfig {

    @Bean
    fun kgsRestClient(): RestClient = RestClient.builder().baseUrl("http://localhost:8081").build()
}
