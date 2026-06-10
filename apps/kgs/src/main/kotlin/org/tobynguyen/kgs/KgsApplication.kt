package org.tobynguyen.kgs

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.tobynguyen.kgs.config.AppConfig

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(AppConfig::class)
@RestController
class KgsApplication {
    @GetMapping fun index(): String = "Hello world"
}

fun main(args: Array<String>) {
    runApplication<KgsApplication>(*args)
}
