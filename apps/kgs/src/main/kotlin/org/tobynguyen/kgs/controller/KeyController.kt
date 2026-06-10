package org.tobynguyen.kgs.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.tobynguyen.kgs.services.RedisService

@RestController
class KeyController(private val redisService: RedisService) {

    @GetMapping("/key")
    fun getKey(): String {
        return redisService.dequeue() ?: ""
    }

}