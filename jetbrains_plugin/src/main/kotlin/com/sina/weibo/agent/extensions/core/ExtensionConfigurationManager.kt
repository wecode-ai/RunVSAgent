package com.sina.weibo.agent.extensions.core

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import java.io.File
import java.util.Properties

/**
 * Extension configuration manager.
 * Manages configuration for different extensions and persists settings.
 */
@Service(Service.Level.PROJECT)
class ExtensionConfigurationManager(private val project: Project) {

    private val logger = Logger.getInstance(ExtensionConfigurationManager::class.java)

    // Configuration file path
    private val configFile: File
        get() = File(project.basePath ?: "", ".vscode-agent")

    // Current extension ID
    @Volatile
    private var currentExtensionId: String? = null

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
     * Load configuration from file
     */
    private fun loadConfiguration() {
        try {
            if (configFile.exists()) {
                val properties = Properties()
                properties.load(configFile.inputStream())
                currentExtensionId = properties.getProperty("extension.type")
                logger.info("Loaded configuration: current extension = $currentExtensionId")
            } else {
                logger.info("No configuration file found, using default settings")
            }
        } catch (e: Exception) {
            logger.warn("Failed to load configuration", e)
        }
    }

    /**
     * Save configuration to file
     */
    private fun saveConfiguration() {
        try {
            val properties = Properties()
            currentExtensionId?.let { properties.setProperty("extension.type", it) }

            // Ensure directory exists
            configFile.parentFile?.mkdirs()

            properties.store(configFile.outputStream(), "RunVSAgent Extension Configuration")
            logger.info("Configuration saved: current extension = $currentExtensionId")
        } catch (e: Exception) {
            logger.warn("Failed to save configuration", e)
        }
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
        saveConfiguration()
    }

    /**
     * Get configuration for a specific extension
     */
    fun getExtensionConfiguration(extensionId: String): Map<String, String> {
        return try {
            val basePath = project.basePath ?: ""
            val extensionConfigFile = File(basePath, ".vscode-agent.$extensionId")
            if (extensionConfigFile.exists()) {
                val properties = Properties()
                properties.load(extensionConfigFile.inputStream())
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
            val basePath = project.basePath ?: ""
            val extensionConfigFile = File(basePath, ".vscode-agent.$extensionId")

            // Ensure directory exists
            extensionConfigFile.parentFile?.mkdirs()

            val properties = Properties()
            config.forEach { (key, value) ->
                properties.setProperty(key, value)
            }

            properties.store(extensionConfigFile.outputStream(), "Extension Configuration for $extensionId")
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
            val basePath = project.basePath ?: ""
            if (basePath.isNotEmpty()) {
                val baseDir = File(basePath)
                if (baseDir.exists() && baseDir.isDirectory) {
                    val files = baseDir.listFiles { file ->
                        file.name.startsWith(".vscode-agent.") && file.name != ".vscode-agent"
                    }
                    files?.forEach { file ->
                        val extensionId = file.name.substring(".vscode-agent.".length)
                        val config = getExtensionConfiguration(extensionId)
                        if (config.isNotEmpty()) {
                            configs[extensionId] = config
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logger.warn("Failed to get all extension configurations", e)
        }

        return configs
    }

    /**
     * Dispose the configuration manager
     */
    fun dispose() {
        logger.info("Disposing extension configuration manager")
        saveConfiguration()
    }
}