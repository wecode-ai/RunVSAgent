// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.extensions.roo

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import java.util.Properties
import java.io.File

/**
 * Extension configuration manager for Roo Code
 * Manages configuration for different extension types
 */
@Service(Service.Level.PROJECT)
class ExtensionConfiguration(private val project: Project) {
    private val LOG = Logger.getInstance(ExtensionConfiguration::class.java)
    
    // Current active extension type
    @Volatile
    private var currentExtensionType: ExtensionType = ExtensionType.getDefault()
    
    // Extension configurations cache
    private val extensionConfigs = mutableMapOf<ExtensionType, ExtensionConfig>()
    
    companion object {
        /**
         * Get extension configuration instance
         */
        fun getInstance(project: Project): ExtensionConfiguration {
            return project.getService(ExtensionConfiguration::class.java)
                ?: error("ExtensionConfiguration not found")
        }
    }
    
    /**
     * Initialize extension configuration
     */
    fun initialize() {
        LOG.info("Initializing extension configuration")
        
        // Load configurations for all extension types
        ExtensionType.getAllTypes().forEach { extensionType ->
            loadConfiguration(extensionType)
        }
        
        // Set current extension type from properties or use default
        val configuredType = getConfiguredExtensionType()
        currentExtensionType = configuredType ?: ExtensionType.getDefault()
        
        LOG.info("Extension configuration initialized, current type: ${currentExtensionType.code}")
    }
    
    /**
     * Get current active extension type
     */
    fun getCurrentExtensionType(): ExtensionType {
        return currentExtensionType
    }
    
    /**
     * Set current active extension type
     */
    fun setCurrentExtensionType(extensionType: ExtensionType) {
        LOG.info("Switching extension type from ${currentExtensionType.code} to ${extensionType.code}")
        currentExtensionType = extensionType
        saveCurrentExtensionType()
    }
    
    /**
     * Get configuration for current extension type
     */
    fun getCurrentConfig(): ExtensionConfig {
        return getConfig(currentExtensionType)
    }
    
    /**
     * Get configuration for specific extension type
     */
    fun getConfig(extensionType: ExtensionType): ExtensionConfig {
        return extensionConfigs[extensionType] ?: ExtensionConfig.getDefault(extensionType)
    }
    
    /**
     * Load configuration for specific extension type
     */
    private fun loadConfiguration(extensionType: ExtensionType) {
        try {
            val config = ExtensionConfig.loadFromProperties(extensionType)
            extensionConfigs[extensionType] = config
            LOG.info("Loaded configuration for ${extensionType.code}")
        } catch (e: Exception) {
            LOG.warn("Failed to load configuration for ${extensionType.code}, using default", e)
            extensionConfigs[extensionType] = ExtensionConfig.getDefault(extensionType)
        }
    }
    
    /**
     * Get configured extension type from properties
     */
    private fun getConfiguredExtensionType(): ExtensionType? {
        return try {
            val properties = Properties()
            val configFile = File(project.basePath ?: "", ".vscode-agent")
            if (configFile.exists()) {
                properties.load(configFile.inputStream())
                val typeCode = properties.getProperty("extension.type")
                if (typeCode != null) {
                    ExtensionType.fromCode(typeCode)
                } else null
            } else null
        } catch (e: Exception) {
            LOG.warn("Failed to read extension type configuration", e)
            null
        }
    }
    
    /**
     * Save current extension type to properties
     */
    private fun saveCurrentExtensionType() {
        try {
            val properties = Properties()
            properties.setProperty("extension.type", currentExtensionType.code)
            
            val configFile = File(project.basePath ?: "", ".vscode-agent")
            properties.store(configFile.outputStream(), "VSCode Agent Configuration")
            
            LOG.info("Saved extension type configuration: ${currentExtensionType.code}")
        } catch (e: Exception) {
            LOG.error("Failed to save extension type configuration", e)
        }
    }
}

/**
 * Extension configuration data class for Roo Code
 */
data class ExtensionConfig(
    val extensionType: ExtensionType,
    val codeDir: String,
    val displayName: String,
    val description: String,
    val publisher: String,
    val version: String,
    val mainFile: String,
    val activationEvents: List<String>,
    val engines: Map<String, String>,
    val capabilities: Map<String, Any>,
    val extensionDependencies: List<String>
) {
    companion object {
        /**
         * Get default configuration for extension type
         */
        fun getDefault(extensionType: ExtensionType): ExtensionConfig {
            return when (extensionType) {
                ExtensionType.ROO_CODE -> ExtensionConfig(
                    extensionType = extensionType,
                    codeDir = "roo-code",
                    displayName = "Roo Code",
                    description = "AI-powered code assistant",
                    publisher = "WeCode-AI",
                    version = "1.0.0",
                    mainFile = "./dist/extension.js",
                    activationEvents = listOf("onStartupFinished"),
                    engines = mapOf("vscode" to "^1.0.0"),
                    capabilities = emptyMap(),
                    extensionDependencies = emptyList()
                )
                ExtensionType.COPILOT -> ExtensionConfig(
                    extensionType = extensionType,
                    codeDir = "copilot",
                    displayName = "GitHub Copilot",
                    description = "AI pair programming assistant",
                    publisher = "GitHub",
                    version = "1.0.0",
                    mainFile = "./dist/extension.js",
                    activationEvents = listOf("onStartupFinished"),
                    engines = mapOf("vscode" to "^1.0.0"),
                    capabilities = emptyMap(),
                    extensionDependencies = emptyList()
                )
                ExtensionType.CLAUDE -> ExtensionConfig(
                    extensionType = extensionType,
                    codeDir = "claude",
                    displayName = "Claude",
                    description = "Anthropic's AI assistant",
                    publisher = "Anthropic",
                    version = "1.0.0",
                    mainFile = "./dist/extension.js",
                    activationEvents = listOf("onStartupFinished"),
                    engines = mapOf("vscode" to "^1.0.0"),
                    capabilities = emptyMap(),
                    extensionDependencies = emptyList()
                )
                ExtensionType.CUSTOM -> ExtensionConfig(
                    extensionType = extensionType,
                    codeDir = "custom",
                    displayName = "Custom Extension",
                    description = "Custom AI extension",
                    publisher = "Custom",
                    version = "1.0.0",
                    mainFile = "./dist/extension.js",
                    activationEvents = listOf("onStartupFinished"),
                    engines = mapOf("vscode" to "^1.0.0"),
                    capabilities = emptyMap(),
                    extensionDependencies = emptyList()
                )
            }
        }
        
        /**
         * Load configuration from properties file
         */
        fun loadFromProperties(extensionType: ExtensionType): ExtensionConfig {
            // This would load from a properties file specific to the extension type
            // For now, return default configuration
            return getDefault(extensionType)
        }
    }
} 