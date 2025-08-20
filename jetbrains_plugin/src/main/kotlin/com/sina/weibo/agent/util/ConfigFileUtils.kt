// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.util

import java.io.File
import java.util.Properties
import java.io.IOException

/**
 * Configuration file utility class
 * Provides unified methods for working with configuration files
 */
object ConfigFileUtils {

    fun getCurrentExtensionId(): String? {
        val properties = loadMainConfig()
        val currentExtensionId = properties.getProperty(PluginConstants.ConfigFiles.EXTENSION_TYPE_KEY)
        return currentExtensionId
    }
    
    /**
     * Ensure configuration directory exists
     */
    fun ensureConfigDirExists() {
        try {
            val configDir = File(PluginConstants.ConfigFiles.getUserConfigDir())
            if (!configDir.exists()) {
                configDir.mkdirs()
                println("Created configuration directory: ${configDir.absolutePath}")
            }
        } catch (e: Exception) {
            throw IOException("Failed to create configuration directory", e)
        }
    }
    
    /**
     * Load properties from main configuration file
     */
    fun loadMainConfig(): Properties {
        val properties = Properties()
        try {
            val configFile = File(PluginConstants.ConfigFiles.getMainConfigPath())
            if (configFile.exists()) {
                properties.load(configFile.inputStream())
            }
        } catch (e: IOException) {
            throw IOException("Failed to load main configuration file", e)
        }
        return properties
    }
    
    /**
     * Save properties to main configuration file
     */
    fun saveMainConfig(properties: Properties, comment: String = "RunVSAgent Configuration") {
        try {
            // Ensure configuration directory exists
            ensureConfigDirExists()
            
            val configFile = File(PluginConstants.ConfigFiles.getMainConfigPath())
            properties.store(configFile.outputStream(), comment)
        } catch (e: IOException) {
            throw IOException("Failed to save main configuration file", e)
        }
    }
    
    /**
     * Load properties from extension-specific configuration file
     */
    fun loadExtensionConfig(extensionId: String): Properties {
        val properties = Properties()
        try {
            val configFile = File(PluginConstants.ConfigFiles.getExtensionConfigPath(extensionId))
            if (configFile.exists()) {
                properties.load(configFile.inputStream())
            }
        } catch (e: IOException) {
            throw IOException("Failed to load extension configuration file for: $extensionId", e)
        }
        return properties
    }
    
    /**
     * Save properties to extension-specific configuration file
     */
    fun saveExtensionConfig(extensionId: String, properties: Properties, comment: String = "Extension Configuration for $extensionId") {
        try {
            // Ensure configuration directory exists
            ensureConfigDirExists()
            
            val configFile = File(PluginConstants.ConfigFiles.getExtensionConfigPath(extensionId))
            properties.store(configFile.outputStream(), comment)
        } catch (e: IOException) {
            throw IOException("Failed to save extension configuration file for: $extensionId", e)
        }
    }
    
    /**
     * Get main configuration file path
     */
    fun getMainConfigPath(): String {
        return PluginConstants.ConfigFiles.getMainConfigPath()
    }
    
    /**
     * Get extension configuration file path
     */
    fun getExtensionConfigPath(extensionId: String): String {
        return PluginConstants.ConfigFiles.getExtensionConfigPath(extensionId)
    }
    
    /**
     * Check if main configuration file exists
     */
    fun mainConfigExists(): Boolean {
        val configFile = File(PluginConstants.ConfigFiles.getMainConfigPath())
        return configFile.exists()
    }
    
    /**
     * Check if extension configuration file exists
     */
    fun extensionConfigExists(extensionId: String): Boolean {
        val configFile = File(PluginConstants.ConfigFiles.getExtensionConfigPath(extensionId))
        return configFile.exists()
    }
    
    /**
     * Get configuration value with default
     */
    fun getConfigValue(key: String, defaultValue: String? = null): String? {
        return try {
            val properties = loadMainConfig()
            properties.getProperty(key, defaultValue)
        } catch (e: Exception) {
            defaultValue
        }
    }
    
    /**
     * Set configuration value
     */
    fun setConfigValue(key: String, value: String) {
        try {
            val properties = loadMainConfig()
            properties.setProperty(key, value)
            saveMainConfig(properties)
        } catch (e: Exception) {
            throw IOException("Failed to set configuration value: $key", e)
        }
    }
    
    /**
     * Create default main configuration file
     */
    fun createDefaultMainConfig() {
        val properties = Properties()
        properties.setProperty(PluginConstants.ConfigFiles.EXTENSION_TYPE_KEY, "roo-code")
        properties.setProperty("# Available extension types:", "")
        properties.setProperty("# - roo-code: Roo Code extension", "")
        properties.setProperty("# - cline: Cline AI extension", "")
        properties.setProperty("# - custom: Custom extension", "")
        
        saveMainConfig(properties, "RunVSAgent Extension Configuration - Default Template")
    }
    
    /**
     * List all extension configuration files
     */
    fun listExtensionConfigFiles(): List<String> {
        val extensionIds = mutableListOf<String>()
        try {
            val configDir = File(PluginConstants.ConfigFiles.getUserConfigDir())
            if (configDir.exists() && configDir.isDirectory) {
                val files = configDir.listFiles { file ->
                    PluginConstants.ConfigFiles.isExtensionConfigFile(file.name)
                }
                files?.forEach { file ->
                    val extensionId = PluginConstants.ConfigFiles.getExtensionIdFromFilename(file.name)
                    if (extensionId != null) {
                        extensionIds.add(extensionId)
                    }
                }
            }
        } catch (e: Exception) {
            // Log error but don't throw
        }
        return extensionIds
    }
}
