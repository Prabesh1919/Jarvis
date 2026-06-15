package com.jarvis.extensibility

import android.content.Context
import com.jarvis.brain.Tool
import com.jarvis.brain.ToolParameter
import com.jarvis.brain.ToolRegistry
import com.jarvis.brain.ToolResult
import java.util.concurrent.ConcurrentHashMap

/**
 * Host interface enabling external applications or modules to dynamically register
 * custom tool plugins into the Jarvis assistant system at runtime.
 */
object PluginRegistry {

    /**
     * Tracks externally registered plugin metadata for auditing and management.
     */
    private val pluginMeta = ConcurrentHashMap<String, PluginMetadata>()

    /**
     * Registers an external plugin tool into the central ToolRegistry.
     * The tool becomes immediately available for invocation via IntentRouter.
     *
     * @param metadata Plugin descriptor containing source identity and capabilities.
     * @param tool The Tool implementation to register.
     * @return true if registered successfully, false if a tool with the same ID already exists.
     */
    fun registerPlugin(metadata: PluginMetadata, tool: Tool): Boolean {
        if (ToolRegistry.getTool(tool.id) != null) {
            return false // Prevent overwriting existing built-in tools
        }

        ToolRegistry.registerTool(tool)
        pluginMeta[tool.id] = metadata
        return true
    }

    /**
     * Unregisters a plugin tool by its tool ID.
     * Only externally registered plugins can be unregistered (not built-in tools).
     */
    fun unregisterPlugin(toolId: String): Boolean {
        if (!pluginMeta.containsKey(toolId)) {
            return false // Not an external plugin
        }

        ToolRegistry.unregisterTool(toolId)
        pluginMeta.remove(toolId)
        return true
    }

    /**
     * Returns metadata for all externally registered plugins.
     */
    fun getRegisteredPlugins(): Collection<PluginMetadata> {
        return pluginMeta.values
    }

    /**
     * Checks if a given tool ID is an external plugin (vs. a built-in tool).
     */
    fun isPlugin(toolId: String): Boolean {
        return pluginMeta.containsKey(toolId)
    }

    /**
     * Creates a simple plugin tool from a lambda function for quick registration.
     * Useful for lightweight integrations that don't need a full Tool subclass.
     */
    fun createSimpleTool(
        id: String,
        name: String,
        description: String,
        parameters: Map<String, ToolParameter> = emptyMap(),
        executor: suspend (Context, Map<String, Any>) -> ToolResult
    ): Tool {
        return object : Tool {
            override val id: String = id
            override val name: String = name
            override val description: String = description
            override val parameters: Map<String, ToolParameter> = parameters

            override suspend fun execute(context: Context, args: Map<String, Any>): ToolResult {
                return try {
                    executor(context, args)
                } catch (e: Exception) {
                    ToolResult.Failure("Plugin error: ${e.localizedMessage}")
                }
            }
        }
    }
}

/**
 * Descriptor for an externally registered plugin.
 */
data class PluginMetadata(
    val pluginId: String,
    val pluginName: String,
    val sourcePackage: String,
    val version: String = "1.0",
    val author: String = "Unknown",
    val registeredAt: Long = System.currentTimeMillis()
)
