package org.tobynguyen.solitar.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

/**
 * Thrown when [org.tobynguyen.solitar.service.UrlService.createUrl] exhausts its retry budget
 * without landing a unique short code. Astronomically unlikely at hobby scale (62^7 keyspace);
 * surfaces a clean 500 rather than looping forever. Auto-mapped by the error-handling starter.
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
class CodeGenerationException(
    message: String = "Could not generate a unique short code; please retry."
) : RuntimeException(message)
