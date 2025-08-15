// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.extensions

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import java.io.File
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap

/**
 * Project configuration loader
 * Loads and parses .vscode-agent configuration files
 */
class ProjectConfigLoader {
    
    companion object {
        private val LOG = Logger.getInstance(ProjectConfigLoader::class.java)
        private val configCache = ConcurrentHashMap<String, ProjectConfig>()
        
        /**
         * Load project configuration
         */
        fun loadConfig(project: Project): ProjectConfig {
            val projectPath = project.basePath ?: ""
            
            return configCache.computeIfAbsent(projectPath) { path ->
                try {
                    val configFile = File(path, ".vscode-agent")
                    if (configFile.exists()) {
                        parseConfigFile(configFile)
                    } else {
                        ProjectConfig.getDefault()
                    }
                } catch (e: Exception) {
                    LOG.warn("Failed to load project configuration for $path", e)
                    ProjectConfig.getDefault()
                }
            }
        }
        
        /**
         * Save project configuration
         */
        fun saveConfig(project: Project, config: ProjectConfig): Boolean {
            return try {
                val projectPath = project.basePath ?: return false
                val configFile = File(projectPath, ".vscode-agent")
                
                val properties = Properties()
                
                // Basic settings
                properties.setProperty("extension.type", config.extensionType)
                properties.setProperty("auto.switch", config.autoSwitch.toString())
                properties.setProperty("debug.mode", config.debugMode)
                properties.setProperty("log.level", config.logLevel)
                
                // Extension-specific settings
                config.extensionSettings.forEach { (extId, settings) ->
                    settings.forEach { (key, value) ->
                        properties.setProperty("extension.$extId.$key", value)
                    }
                }
                
                properties.store(configFile.outputStream(), "Extension Configuration")
                
                // Update cache
                configCache[projectPath] = config
                
                LOG.info("Project configuration saved to ${configFile.absolutePath}")
                true
            } catch (e: Exception) {
                LOG.error("Failed to save project configuration", e)
                false
            }
        }
        
        /**
         * Parse configuration file
         */
        private fun parseConfigFile(configFile: File): ProjectConfig {
            val properties = Properties()
            properties.load(configFile.inputStream())
            
            val extensionType = properties.getProperty("extension.type", "cline")
            val autoSwitch = properties.getProperty("auto.switch", "false").toBoolean()
            val debugMode = properties.getProperty("debug.mode", "none")
            val logLevel = properties.getProperty("log.level", "info")
            
            // Parse extension-specific settings
            val extensionSettings = mutableMapOf<String, MutableMap<String, String>>()
            properties.forEach { (key, value) ->
                val keyStr = key.toString()
                if (keyStr.startsWith("extension.") && keyStr.contains(".")) {
                    val parts = keyStr.split(".", limit = 3)
                    if (parts.size >= 3) {
                        val extId = parts[1]
                        val settingKey = parts[2]
                        val currentSettings = extensionSettings[extId] ?: mutableMapOf()
                        currentSettings[settingKey] = value.toString()
                        extensionSettings[extId] = currentSettings
                    }
                }
            }
            
            return ProjectConfig(
                extensionType = extensionType,
                autoSwitch = autoSwitch,
                debugMode = debugMode,
                logLevel = logLevel,
                extensionSettings = extensionSettings
            )
        }
        
        /**
         * Clear configuration cache
         */
        fun clearCache() {
            configCache.clear()
        }
    }
}

/**
 * Project configuration data class
 */
data class ProjectConfig(
    val extensionType: String,
    val autoSwitch: Boolean,
    val debugMode: String,
    val logLevel: String,
    val extensionSettings: Map<String, Map<String, String>>
) {
    companion object {
        fun getDefault(): ProjectConfig {
            return ProjectConfig(
                extensionType = "cline",
                autoSwitch = false,
                debugMode = "none",
                logLevel = "info",
                extensionSettings = emptyMap()
            )
        }
    }
}
