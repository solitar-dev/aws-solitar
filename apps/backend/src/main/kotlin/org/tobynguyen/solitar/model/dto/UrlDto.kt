package org.tobynguyen.solitar.model.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class UrlCreateDto(
    @field:NotBlank(message = "URL is required")
    @field:Pattern(
        message = "Invalid URL",
        regexp =
            "^https?://(?:www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b[-a-zA-Z0-9()@:%_+.~#?&/=]*$",
    )
    val url: String
)

data class UrlResponseDto(val originalUrl: String, val shortCode: String)
