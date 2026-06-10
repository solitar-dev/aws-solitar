package org.tobynguyen.solitar.util

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

/** Pure unit test (no Spring/Docker) for the short-code generator. */
class CodeGeneratorTest {

    private val generator = CodeGenerator()

    @Test
    fun `generates fixed-length codes from the Base62 alphabet`() {
        repeat(2000) {
            val code = generator.generate()
            assertEquals(CodeGenerator.LENGTH, code.length, "wrong length: $code")
            assertTrue(code.all { it in CodeGenerator.ALPHABET }, "non-Base62 char in: $code")
        }
    }

    @Test
    fun `detects reserved words case-insensitively`() {
        assertTrue(generator.isReserved("settings"))
        assertTrue(generator.isReserved("SETTINGS"))
        assertTrue(generator.isReserved("Qr"))
        assertTrue(generator.isReserved("unlock"))
        assertFalse(generator.isReserved("abc1234"))
    }

    @Test
    fun `no reserved word can collide with a fixed-length code`() {
        // Every reserved word has length != LENGTH, so a generated code can never equal one.
        assertTrue(CodeGenerator.RESERVED.none { it.length == CodeGenerator.LENGTH })
    }
}
