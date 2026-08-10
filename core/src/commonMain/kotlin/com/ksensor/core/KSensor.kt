package com.ksensor.core

import com.ksensor.core.model.PluginId

/**
 * Main entry point for KSensor. Handles plugin registration and discovery.
 */
object KSensor {
    private val registry = mutableMapOf<PluginId, KSensorPlugin>()
    private val storage: PlatformStorage by lazy { createPlatformStorage() }

    private val KEY_START_ON_BOOT = "ksensor_start_on_boot"
    private val KEY_BOOT_PLUGINS = "ksensor_boot_plugins"

    /**
     * Flag to enable/disable "start on boot" for the entire library.
     */
    var startOnBoot: Boolean
        get() = storage.getBoolean(KEY_START_ON_BOOT, false)
        set(value) = storage.putBoolean(KEY_START_ON_BOOT, value)

    private val startOnBootPlugins: MutableSet<PluginId> by lazy {
        storage.getStringSet(KEY_BOOT_PLUGINS, emptySet())
            .map { PluginId.valueOf(it) }
            .toMutableSet()
    }

    private fun persistBootPlugins() {
        storage.putStringSet(KEY_BOOT_PLUGINS, startOnBootPlugins.map { it.name }.toSet())
    }

    /**
     * Permission handler for checking and requesting permissions.
     */
    val permissionHandler: PermissionHandler by lazy { createPermissionHandler() }

    /**
     * Registers a plugin.
     * @param startOnBoot If true, this plugin will be marked to start automatically on device boot.
     */
    fun register(plugin: KSensorPlugin, startOnBoot: Boolean = false) {
        if (registry.containsKey(plugin.id)) return

        registry[plugin.id] = plugin
        if (startOnBoot) {
            startOnBootPlugins.add(plugin.id)
            persistBootPlugins()
        }
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

    /**
     * Returns the list of plugin IDs that are marked to start on boot.
     */
    fun getStartOnBootPlugins(): Set<PluginId> = startOnBootPlugins.toSet()

    /**
     * Marks a plugin to start on boot or removes the mark.
     */
    fun setStartOnBoot(id: PluginId, enable: Boolean) {
        if (enable) {
            startOnBootPlugins.add(id)
        } else {
            startOnBootPlugins.remove(id)
        }
        persistBootPlugins()
    }
}
