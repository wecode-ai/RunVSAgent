package com.sina.weibo.agent.extensions.core

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import java.io.File
import java.util.Properties
import com.sina.weibo.agent.util.PluginConstants
import com.sina.weibo.agent.util.ConfigFileUtils

/**
 * Extension configuration manager.
 * Manages configuration for different extensions and persists settings.
 */
@Service(Service.Level.PROJECT)
class ExtensionConfigurationManager(private val project: Project) {

    private val logger = Logger.getInstance(ExtensionConfigurationManager::class.java)

    // Configuration file path
    private val configFile: File
        get() = File(PluginConstants.ConfigFiles.getMainConfigPath())

    // Current extension ID
    @Volatile
    private var currentExtensionId: String? = null
    
    // Configuration validation status
    @Volatile
    private var isConfigurationValid = false
    
    // Configuration loading status
    @Volatile
    private var isConfigurationLoaded = false
    
    // Configuration loading time
    private var configurationLoadTime: Long? = null

    companion object {
        /**
         * Get extension configuration manager instance
         */
        fun getInstance(project: Project): ExtensionConfigurationManager {
            return project.getService(ExtensionConfigurationManager::class.java)
                ?: error("ExtensionConfigurationManager not found")
        }
    }

    /**
     * Initialize the configuration manager
     */
    fun initialize() {
        logger.info("Initializing extension configuration manager")
        loadConfiguration()
    }
    
    /**
     * Check if configuration is valid and ready for use
     */
    fun isConfigurationValid(): Boolean {
        return isConfigurationValid
    }
    
    /**
     * Check if configuration has been loaded
     */
    fun isConfigurationLoaded(): Boolean {
        return isConfigurationLoaded
    }
    
    /**
     * Get configuration loading time
     */
    fun getConfigurationLoadTime(): Long? {
        return configurationLoadTime
    }
    
    /**
     * Get configuration validation error message if any
     */
    fun getConfigurationError(): String? {
        return if (isConfigurationLoaded && !isConfigurationValid) {
            val extensionId = currentExtensionId // Create local variable to avoid concurrency issues
            when {
                extensionId == null -> "No extension type configured. Please set '${PluginConstants.ConfigFiles.EXTENSION_TYPE_KEY}' in ${PluginConstants.ConfigFiles.MAIN_CONFIG_FILE} file"
                extensionId.isBlank() -> "Extension type is empty. Please set a valid value for '${PluginConstants.ConfigFiles.EXTENSION_TYPE_KEY}' in ${PluginConstants.ConfigFiles.MAIN_CONFIG_FILE} file"
                else -> "Invalid extension type: '$extensionId'. Please check the value of '${PluginConstants.ConfigFiles.EXTENSION_TYPE_KEY}' in ${PluginConstants.ConfigFiles.MAIN_CONFIG_FILE} file"
            }
        } else null
    }
    
    /**
     * Get detailed configuration status information
     */
    fun getConfigurationStatus(): String {
        return buildString {
            append("Configuration Status: ")
            if (isConfigurationLoaded) {
                if (isConfigurationValid) {
                    val extensionId = currentExtensionId // Create local variable to avoid concurrency issues
                    append("VALID ($extensionId)")
                } else {
                    append("INVALID - ${getConfigurationError()}")
                }
            } else {
                append("LOADING")
            }
            append(" | File: ${getConfigurationFilePath()}")
        }
    }
    
    /**
     * Get detailed debug information for troubleshooting
     */
    fun getDebugInfo(): String {
        return buildString {
            append("=== Configuration Debug Info ===\n")
            append("Configuration loaded: $isConfigurationLoaded\n")
            append("Configuration valid: $isConfigurationValid\n")
            append("Current extension ID: $currentExtensionId\n")
            append("Config file path: ${getConfigurationFilePath()}\n")
            append("Config file exists: ${configFile.exists()}\n")
            
            if (configFile.exists()) {
                append("Config file size: ${configFile.length()} bytes\n")
                append("Config file last modified: ${java.util.Date(configFile.lastModified())}\n")
                
                try {
                    val properties = Properties()
                    properties.load(configFile.inputStream())
                    append("Config file content:\n")
                    properties.stringPropertyNames().forEach { key ->
                        append("  $key = ${properties.getProperty(key)}\n")
                    }
                } catch (e: Exception) {
                    append("Failed to read config file: ${e.message}\n")
                }
            }
            
            append("Project base path: ${project.basePath}\n")
            append("================================")
        }
    }
    
    /**
     * Get recovery suggestions for invalid configuration
     */
    fun getRecoverySuggestions(): List<String> {
        return if (isConfigurationLoaded && !isConfigurationValid) {
            listOf(
                "1. Check if ${PluginConstants.ConfigFiles.MAIN_CONFIG_FILE} file exists in project root",
                "2. Ensure '${PluginConstants.ConfigFiles.EXTENSION_TYPE_KEY}' property is set to a valid extension ID",
                "3. Valid extension types: roo-code, cline, custom",
                "4. Try running 'createDefaultConfiguration()' to generate a template",
                "5. Check file permissions and ensure the file is readable"
            )
        } else {
            emptyList()
        }
    }

    /**
     * Load configuration from file
     */
    private fun loadConfiguration() {
        try {
            isConfigurationLoaded = false
            isConfigurationValid = false
            configurationLoadTime = System.currentTimeMillis()
            
            if (ConfigFileUtils.mainConfigExists()) {
                val properties = ConfigFileUtils.loadMainConfig()
                currentExtensionId = properties.getProperty(PluginConstants.ConfigFiles.EXTENSION_TYPE_KEY)
                
                // Validate configuration
                isConfigurationValid = validateConfiguration(currentExtensionId)
                
                if (isConfigurationValid) {
                    logger.info("Loaded valid configuration: current extension = $currentExtensionId")
                } else {
                    logger.warn("Configuration loaded but invalid: extension type is null or empty")
                }
            } else {
                logger.warn("No configuration file found at: ${configFile.absolutePath}")
                currentExtensionId = null
                isConfigurationValid = false
            }
            
            isConfigurationLoaded = true
        } catch (e: Exception) {
            logger.error("Failed to load configuration", e)
            currentExtensionId = null
            isConfigurationValid = false
            isConfigurationLoaded = true
        }
    }
    
    /**
     * Validate configuration
     */
    private fun validateConfiguration(extensionId: String?): Boolean {
        return !extensionId.isNullOrBlank()
    }

    /**
     * Save configuration to file
     */
    private fun saveConfiguration() {
        try {
            val properties = Properties()
            currentExtensionId?.let { properties.setProperty(PluginConstants.ConfigFiles.EXTENSION_TYPE_KEY, it) }

            logger.info("Saving configuration to file: ${configFile.absolutePath}")
            logger.info("Configuration content: ${PluginConstants.ConfigFiles.EXTENSION_TYPE_KEY}=$currentExtensionId")
            
            ConfigFileUtils.saveMainConfig(properties)
            
            // Verify the file was created and contains the expected content
            if (configFile.exists()) {
                val savedProperties = Properties()
                savedProperties.load(configFile.inputStream())
                val savedExtensionId = savedProperties.getProperty(PluginConstants.ConfigFiles.EXTENSION_TYPE_KEY)
                logger.info("Configuration saved successfully. File content: ${PluginConstants.ConfigFiles.EXTENSION_TYPE_KEY}=$savedExtensionId")
            } else {
                logger.error("Configuration file was not created after save operation")
            }
        } catch (e: Exception) {
            logger.error("Failed to save configuration", e)
            throw e // Re-throw to let caller know about the failure
        }
    }

    /**
     * Reload configuration from file
     */
    fun reloadConfiguration() {
        logger.info("Reloading configuration")
        loadConfiguration()
    }
    
    /**
     * Check if configuration has changed and reload if necessary
     */
    fun checkConfigurationChange() {
        try {
            if (configFile.exists()) {
                val lastModified = configFile.lastModified()
                // Simple change detection - could be enhanced with file watcher
                if (lastModified > (System.currentTimeMillis() - 5000)) { // Check if modified in last 5 seconds
                    logger.info("Configuration file changed, reloading...")
                    reloadConfiguration()
                }
            }
        } catch (e: Exception) {
            logger.warn("Error checking configuration change", e)
        }
    }
    
    /**
     * Get configuration file path for user reference
     */
    fun getConfigurationFilePath(): String {
        return configFile.absolutePath
    }
    
    /**
     * Get current extension ID
     */
    fun getCurrentExtensionId(): String? {
        return currentExtensionId
    }

    /**
     * Set current extension ID
     */
    fun setCurrentExtensionId(extensionId: String) {
        logger.info("Setting current extension ID to: $extensionId")
        currentExtensionId = extensionId
        
        // Save configuration to file
        saveConfiguration()
        
        // Reload configuration to ensure it's properly loaded and validated
        reloadConfiguration()
        
        logger.info("Extension ID set and configuration reloaded: $extensionId, valid: $isConfigurationValid")
    }

    /**
     * Get configuration for a specific extension
     */
    fun getExtensionConfiguration(extensionId: String): Map<String, String> {
        return try {
            if (ConfigFileUtils.extensionConfigExists(extensionId)) {
                val properties = ConfigFileUtils.loadExtensionConfig(extensionId)
                properties.stringPropertyNames().associateWith { properties.getProperty(it) }
            } else {
                emptyMap()
            }
        } catch (e: Exception) {
            logger.warn("Failed to load extension configuration for: $extensionId", e)
            emptyMap()
        }
    }

    /**
     * Set configuration for a specific extension
     */
    fun setExtensionConfiguration(extensionId: String, config: Map<String, String>) {
        try {
            val properties = Properties()
            config.forEach { (key, value) ->
                properties.setProperty(key, value)
            }

            ConfigFileUtils.saveExtensionConfig(extensionId, properties)
            logger.info("Configuration saved for extension: $extensionId")
        } catch (e: Exception) {
            logger.warn("Failed to save extension configuration for: $extensionId", e)
        }
    }

    /**
     * Get all available extension configurations
     */
    fun getAllExtensionConfigurations(): Map<String, Map<String, String>> {
        val configs = mutableMapOf<String, Map<String, String>>()

        try {
            val extensionIds = ConfigFileUtils.listExtensionConfigFiles()
            extensionIds.forEach { extensionId ->
                val config = getExtensionConfiguration(extensionId)
                if (config.isNotEmpty()) {
                    configs[extensionId] = config
                }
            }
        } catch (e: Exception) {
            logger.warn("Failed to get all extension configurations", e)
        }

        return configs
    }

    /**
     * Create default configuration file with template
     */
    fun createDefaultConfiguration() {
        try {
            if (!ConfigFileUtils.mainConfigExists()) {
                ConfigFileUtils.createDefaultMainConfig()
                logger.info("Created default configuration file at: ${configFile.absolutePath}")
                
                // Reload configuration
                reloadConfiguration()
            }
        } catch (e: Exception) {
            logger.error("Failed to create default configuration", e)
        }
    }

    /**
     * Dispose the configuration manager
     */
    fun dispose() {
        logger.info("Disposing extension configuration manager")
        saveConfiguration()
    }
}