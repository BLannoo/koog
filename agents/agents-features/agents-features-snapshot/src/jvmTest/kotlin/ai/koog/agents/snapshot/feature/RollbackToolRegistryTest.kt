package ai.koog.agents.snapshot.feature

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolResult
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class RollbackToolRegistryTest {

    @Serializable
    private data class Args(val value: String)

    private val toolA = object : Tool<Args, ToolResult.Text>() {
        override val descriptor = ToolDescriptor(name = "tool_a", description = "Tool A")
        override val argsSerializer = Args.serializer()
        override suspend fun execute(args: Args) = ToolResult.Text("a")
    }

    private val rollbackA = object : Tool<Args, ToolResult.Text>() {
        override val descriptor = ToolDescriptor(name = "rollback_a", description = "Rollback A")
        override val argsSerializer = Args.serializer()
        override suspend fun execute(args: Args) = ToolResult.Text("rollback-a")
    }

    // ── KG-676 regression tests ───────────────────────────────────────────────
    //
    // RollbackToolRegistry had the same bug as ToolRegistry: EMPTY was a stored
    // singleton with a mutable backing map. Any call to add() on a reference
    // obtained from EMPTY would corrupt the shared object.

    /**
     * Two independent callers both obtain RollbackToolRegistry.EMPTY.
     * One of them registers a rollback pair (the KG-676 anti-pattern).
     * The other caller must still see an empty registry.
     */
    @Test
    fun testEmptyRollbackRegistryNotSharedBetweenCallers() {
        val callerA = RollbackToolRegistry.EMPTY
        val callerB = RollbackToolRegistry.EMPTY

        callerA.add(toolA, rollbackA)  // callerA mutates its reference

        assertTrue(
            callerB.rollbackToolsMap.isEmpty(),
            "RollbackToolRegistry.EMPTY must not be a shared mutable singleton: " +
                "callerA.add() corrupted callerB's registry"
        )
    }

    /**
     * Each access to RollbackToolRegistry.EMPTY must return a distinct object.
     */
    @Test
    fun testEmptyRollbackRegistryAccessesAreDistinctInstances() {
        val first = RollbackToolRegistry.EMPTY
        val second = RollbackToolRegistry.EMPTY

        assertNotSame(
            first, second,
            "RollbackToolRegistry.EMPTY must return a new instance on each access " +
                "to prevent shared mutable state (KG-676)"
        )
    }
}
