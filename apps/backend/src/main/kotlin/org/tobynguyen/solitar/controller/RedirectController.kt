package org.tobynguyen.solitar.controller

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.tobynguyen.solitar.exception.UrlNotFoundException
import org.tobynguyen.solitar.service.UrlService

/**
 * Primary redirect entry point: `GET /{code}` → `301 Location: <originalUrl>`. CloudFront routes
 * the default `*` behavior here (uncached) so every hit reaches the backend and counts toward
 * stats. Stored targets are http(s)-only (enforced at create time), so the Location header is set
 * raw to avoid `URI.create` parse failures on otherwise-valid URLs.
 */
@RestController
class RedirectController(private val urlService: UrlService) {

    @GetMapping("/{code}")
    fun redirect(@PathVariable code: String): ResponseEntity<Void> {
        if (!CODE_PATTERN.matches(code)) {
            throw UrlNotFoundException("Short URL with code '$code' not found.")
        }

        val entity = urlService.resolve(code)

        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
            .header(HttpHeaders.LOCATION, entity.originalUrl)
            .build()
    }

    private companion object {
        val CODE_PATTERN = Regex("^[A-Za-z0-9]{1,32}$")
    }
}
