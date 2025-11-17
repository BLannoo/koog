package ai.koog.agents.core.tools

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ToolDescriptorValidationTest {

    @Test
    fun `creating ToolDescriptor with empty name throws IllegalArgumentException`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            ToolDescriptor(name = "", description = "A valid description")
        }
        assertEquals("Tool name cannot be blank.", exception.message)
    }

    @Test
    fun `creating ToolDescriptor with empty description throws IllegalArgumentException`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            ToolDescriptor(name = "a_valid_name", description = "")
        }
        assertEquals("Tool description cannot be blank.", exception.message)
    }
}
