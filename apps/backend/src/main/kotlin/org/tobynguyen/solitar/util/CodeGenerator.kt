package org.tobynguyen.solitar.util

import java.util.concurrent.ThreadLocalRandom
import org.springframework.stereotype.Component

/**
 * Short-code minting (replaces the deleted KGS service). Generates a random fixed-length Base62
 * code; uniqueness is enforced by the caller's conditional DynamoDB write + retry, not here.
 * Injectable so tests can substitute a deterministic generator to exercise the collision path.
 */
@Component
class CodeGenerator {

    /** A random [LENGTH]-character Base62 code. */
    fun generate(): String {
        val random = ThreadLocalRandom.current()
        return buildString { repeat(LENGTH) { append(ALPHABET[random.nextInt(ALPHABET.length)]) } }
    }

    /** True if the code collides (case-insensitively) with a reserved frontend route word. */
    fun isReserved(code: String): Boolean = code.lowercase() in RESERVED

    companion object {
        const val LENGTH = 7
        const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

        /**
         * Frontend route words a generated code must never shadow (defense-in-depth with P4 edge).
         */
        val RESERVED = setOf("settings", "qr", "unlock", "index", "error")
    }
}
