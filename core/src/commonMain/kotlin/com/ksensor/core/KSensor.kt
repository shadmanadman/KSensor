package com.ksensor.core

import com.ksensor.core.model.PluginId

/**
 * Main entry point for KSensor. Handles plugin registration and discovery.
 */
object KSensor {
    private val registry = mutableMapOf<PluginId, KSensorPlugin>()

    /**
     * Permission handler for checking and requesting permissions.
     */
    val permissionHandler: PermissionHandler by lazy { createPermissionHandler() }

    /**
     * Registers a plugin.
     */
    fun register(plugin: KSensorPlugin) {
        if (registry.containsKey(plugin.id)) return

        registry[plugin.id] = plugin
    }

    /**
     * Unregisters a plugin.
     */
    fun unregister(id: PluginId) {
        if (registry.containsKey(id).not()) throw Exception("Plugin with id $id not found")
        
        registry.remove(id)
    }

    /**
     * Retrieves a registered plugin by its ID.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : KSensorPlugin> get(id: PluginId): T? {
        return registry[id] as? T
    }

    /**
     * Checks if a plugin is registered.
     */
    fun hasPlugin(id: PluginId): Boolean = registry.containsKey(id)
}
