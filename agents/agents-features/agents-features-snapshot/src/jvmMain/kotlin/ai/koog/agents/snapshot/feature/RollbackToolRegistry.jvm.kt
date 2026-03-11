package ai.koog.agents.snapshot.feature

import ai.koog.agents.core.tools.reflect.asTool
import kotlin.reflect.KFunction

/**
 * Adds a tool and its corresponding rollback tool to the registry.
 * This convenience method converts both `toolFunction` and `rollbackToolFunction` into `Tool` objects before adding them.
 *
 * Internal: prefer the [RollbackToolRegistryBuilder] DSL. This extension wraps the internal [RollbackToolRegistry.add]
 * and is only for module-internal use.
 *
 * @param toolFunction The Kotlin function representing the primary tool to add.
 * @param rollbackToolFunction The Kotlin function representing the rollback tool associated with the primary tool.
 */
internal fun RollbackToolRegistry.add(
    toolFunction: KFunction<*>,
    rollbackToolFunction: KFunction<*>
) {
    this.add(toolFunction.asTool(), rollbackToolFunction.asTool())
}
