package com.sina.weibo.agent.extensions.core

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.sina.weibo.agent.core.ExtensionManager
import com.sina.weibo.agent.extensions.common.ExtensionType
import com.sina.weibo.agent.extensions.config.ExtensionConfig
import com.sina.weibo.agent.extensions.config.ExtensionConfiguration
import com.sina.weibo.agent.util.PluginConstants
import com.sina.weibo.agent.util.PluginResourceUtil
import java.io.File

/**
 * Extension manager factory for Roo Code
 * Creates and manages extension managers for different extension types
 */
@Service(Service.Level.PROJECT)
class ExtensionManagerFactory(private val project: Project) {
    private val LOG = Logger.getInstance(ExtensionManagerFactory::class.java)

    // Extension managers cache
    private val extensionManagers = mutableMapOf<ExtensionType, ExtensionManager>()

    companion object {
        /**
         * Get extension manager factory instance
         */
        fun getInstance(project: Project): ExtensionManagerFactory {
            return project.getService(ExtensionManagerFactory::class.java)
                ?: error("ExtensionManagerFactory not found")
        }
    }

    /**
     * Initialize extension manager factory
     */
    fun initialize() {
        LOG.info("Initializing extension manager factory")

        // Get extension configuration
        val extensionConfig = ExtensionConfiguration.Companion.getInstance(project)

        // Create extension managers for all supported types
        ExtensionType.Companion.getAllTypes().forEach { extensionType ->
            createExtensionManager(extensionType, extensionConfig.getConfig(extensionType))
        }

        LOG.info("Extension manager factory initialized")
    }

    /**
     * Get extension manager for current extension type
     */
    fun getCurrentExtensionManager(): ExtensionManager {
        val extensionConfig = ExtensionConfiguration.Companion.getInstance(project)
        val currentType = extensionConfig.getCurrentExtensionType()
        return getExtensionManager(currentType)
    }

    /**
     * Get extension manager for specific extension type
     */
    fun getExtensionManager(extensionType: ExtensionType): ExtensionManager {
        return extensionManagers[extensionType]
            ?: throw IllegalStateException("Extension manager not found for type: ${extensionType.code}")
    }

    /**
     * Create extension manager for specific extension type
     */
    private fun createExtensionManager(extensionType: ExtensionType, config: ExtensionConfig) {
        LOG.info("Creating extension manager for type: ${extensionType.code}")

        val extensionManager = ExtensionManager()

        // Try to register extension if it exists
        val extensionPath = getExtensionPath(config)
        if (extensionPath != null && File(extensionPath).exists()) {
            try {
                extensionManager.registerExtension(extensionPath, config)
                LOG.info("Extension registered for type: ${extensionType.code}")
                extensionManagers[extensionType] = extensionManager
            } catch (e: Exception) {
                LOG.warn("Failed to register extension for type: ${extensionType.code}", e)
            }
        } else {
            LOG.info("Extension path not found for type: ${extensionType.code}: $extensionPath")
        }
    }

    /**
     * Get extension path for configuration
     */
    private fun getExtensionPath(config: ExtensionConfig): String? {
        // Use PluginResourceUtil to get extension path (this is the correct way)
        try {
            val extensionPath = PluginResourceUtil.getResourcePath(
                PluginConstants.PLUGIN_ID,
                config.codeDir
            )
            if (extensionPath != null && File(extensionPath).exists()) {
                LOG.info("Found extension path via PluginResourceUtil: $extensionPath")
                return extensionPath
            }
        } catch (e: Exception) {
            LOG.warn("Failed to get extension path via PluginResourceUtil for: ${config.codeDir}", e)
        }

        LOG.warn("Extension path not found for type: ${config.extensionType.code} (${config.codeDir})")
        return null
    }

    /**
     * Switch to different extension type
     */
    fun switchExtensionType(extensionType: ExtensionType) {
        LOG.info("Switching to extension type: ${extensionType.code}")

        val extensionConfig = ExtensionConfiguration.Companion.getInstance(project)
        extensionConfig.setCurrentExtensionType(extensionType)

        // Re-initialize with new configuration
        val config = extensionConfig.getConfig(extensionType)
        createExtensionManager(extensionType, config)

        LOG.info("Switched to extension type: ${extensionType.code}")
    }

    /**
     * Get all available extension types
     */
    fun getAvailableExtensionTypes(): List<ExtensionType> {
        return extensionManagers.keys.toList()
    }

    /**
     * Dispose all extension managers
     */
    fun dispose() {
        LOG.info("Disposing extension manager factory")
        extensionManagers.values.forEach { it.dispose() }
        extensionManagers.clear()
    }
}