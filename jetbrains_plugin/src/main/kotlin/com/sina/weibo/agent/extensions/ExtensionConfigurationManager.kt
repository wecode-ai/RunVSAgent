// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.extensions

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.PersistentStateComponent

import java.io.File
import java.util.Properties

/**
 * Extension configuration manager
 * Manages extension configuration and persistence
 */
@Service(Service.Level.PROJECT)
@State(
    name = "ExtensionConfigurationManager",
    storages = [Storage("extension-configuration.xml")]
)
class ExtensionConfigurationManager(private val project: Project) : PersistentStateComponent<ExtensionConfigurationManager.State> {
    
    private val LOG = Logger.getInstance(ExtensionConfigurationManager::class.java)
    
    // Configuration state
    data class State(
        var currentExtensionId: String = "cline",
        var previousExtensionId: String? = null,
        var autoSwitchEnabled: Boolean = false,
        var switchHistory: MutableList<String> = mutableListOf(),
        var extensionSettings: MutableMap<String, MutableMap<String, String>> = mutableMapOf()
    )
    
    private var state = State()
    
    companion object {
        fun getInstance(project: Project): ExtensionConfigurationManager {
            return project.getService(ExtensionConfigurationManager::class.java)
                ?: error("ExtensionConfigurationManager not found")
        }
    }
    
    /**
     * Get current extension ID
     */
    fun getCurrentExtensionId(): String = state.currentExtensionId
    
    /**
     * Set current extension ID
     */
    fun setCurrentExtensionId(extensionId: String) {
        if (state.currentExtensionId != extensionId) {
            state.previousExtensionId = state.currentExtensionId
            state.currentExtensionId = extensionId
            
            // Add to switch history
            if (!state.switchHistory.contains(extensionId)) {
                state.switchHistory.add(extensionId)
                // Keep only last 10 switches
                if (state.switchHistory.size > 10) {
                    state.switchHistory.removeAt(0)
                }
            }
            
            LOG.info("Extension ID changed from ${state.previousExtensionId} to $extensionId")
        }
    }
    
    /**
     * Get previous extension ID
     */
    fun getPreviousExtensionId(): String? = state.previousExtensionId
    
    /**
     * Get switch history
     */
    fun getSwitchHistory(): List<String> = state.switchHistory.toList()
    
    /**
     * Check if auto-switch is enabled
     */
    fun isAutoSwitchEnabled(): Boolean = state.autoSwitchEnabled
    
    /**
     * Set auto-switch enabled
     */
    fun setAutoSwitchEnabled(enabled: Boolean) {
        state.autoSwitchEnabled = enabled
    }
    
    /**
     * Get extension-specific settings
     */
    fun getExtensionSettings(extensionId: String): Map<String, String> {
        return state.extensionSettings[extensionId] ?: emptyMap()
    }
    
    /**
     * Set extension-specific settings
     */
    fun setExtensionSettings(extensionId: String, settings: Map<String, String>) {
        state.extensionSettings[extensionId] = settings.toMutableMap()
    }
    
    /**
     * Load configuration from .vscode-agent file
     */
    fun loadFromProjectConfig() {
        try {
            val projectPath = project.basePath ?: return
            val configFile = File(projectPath, ".vscode-agent")
            
            if (configFile.exists()) {
                val properties = Properties()
                properties.load(configFile.inputStream())
                
                val extensionType = properties.getProperty("extension.type")
                if (extensionType != null) {
                    setCurrentExtensionId(extensionType)
                    LOG.info("Loaded extension type from project config: $extensionType")
                }
                
                // Load other settings
                val autoSwitch = properties.getProperty("auto.switch", "false")
                setAutoSwitchEnabled(autoSwitch.toBoolean())
                
                // Load extension-specific settings
                properties.forEach { (key, value) ->
                    val keyStr = key.toString()
                    if (keyStr.startsWith("extension.") && keyStr.contains(".")) {
                        val parts = keyStr.split(".", limit = 3)
                        if (parts.size >= 3) {
                            val extId = parts[1]
                            val settingKey = parts[2]
                            val currentSettings = state.extensionSettings[extId] ?: mutableMapOf()
                            currentSettings[settingKey] = value.toString()
                            state.extensionSettings[extId] = currentSettings
                        }
                    }
                }
            }
        } catch (e: Exception) {
            LOG.warn("Failed to load project configuration", e)
        }
    }
    
    /**
     * Save configuration to .vscode-agent file
     */
    fun saveToProjectConfig() {
        try {
            val projectPath = project.basePath ?: return
            val configFile = File(projectPath, ".vscode-agent")
            
            val properties = Properties()
            properties.setProperty("extension.type", state.currentExtensionId)
            properties.setProperty("auto.switch", state.autoSwitchEnabled.toString())
            
            // Save extension-specific settings
            state.extensionSettings.forEach { (extId, settings) ->
                settings.forEach { (key, value) ->
                    properties.setProperty("extension.$extId.$key", value)
                }
            }
            
            properties.store(configFile.outputStream(), "Extension Configuration")
            LOG.info("Configuration saved to project config file")
        } catch (e: Exception) {
            LOG.warn("Failed to save project configuration", e)
        }
    }
    
    /**
     * Get state for persistence
     */
    override fun getState(): State = state
    
    /**
     * Load state from persistence
     */
    override fun loadState(state: State) {
        this.state = state
        LOG.info("Extension configuration state loaded")
    }
}
