package ai.koog.agents.snapshot.feature

import ai.koog.agents.testing.tools.DummyTool
import kotlin.test.Test
import kotlin.test.assertTrue

class RollbackToolRegistryTest {
    private val tool = DummyTool()
    private val rollbackTool = DummyTool()

    @Test
    fun testEmptyRegistryIsNotMutatedAcrossAccesses() {
        // Regression test for KG-676: RollbackToolRegistry.EMPTY must return a fresh instance
        // each time so that mutating one returned value does not corrupt subsequent accesses.
        val first = RollbackToolRegistry.EMPTY
        first.add(tool, rollbackTool)

        val second = RollbackToolRegistry.EMPTY
        assertTrue(second.rollbackToolsMap.isEmpty(), "EMPTY must return an unpolluted registry after mutation of a previously returned instance")
    }
}
