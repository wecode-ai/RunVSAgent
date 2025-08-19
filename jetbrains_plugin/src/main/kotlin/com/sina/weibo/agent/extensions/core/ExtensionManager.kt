// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.extensions.core

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.sina.weibo.agent.extensions.common.ExtensionChangeListener
import com.sina.weibo.agent.extensions.config.ExtensionProvider
import com.sina.weibo.agent.extensions.plugin.cline.ClineExtensionProvider
import com.sina.weibo.agent.extensions.plugin.roo.RooExtensionProvider
import com.sina.weibo.agent.extensions.ui.buttons.DynamicButtonManager
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * Global extension manager
 * Manages all available extension providers
 */
@Service(Service.Level.PROJECT)
class ExtensionManager(private val project: Project) {
    private val LOG = Logger.getInstance(ExtensionManager::class.java)
    
    // Registered extension providers
    private val extensionProviders = ConcurrentHashMap<String, ExtensionProvider>()
    
    // Current active extension provider
    @Volatile
    private var currentProvider: ExtensionProvider? = null
    
    companion object {
        /**
         * Get extension manager instance
         */
        fun getInstance(project: Project): ExtensionManager {
            return project.getService(ExtensionManager::class.java)
                ?: error("ExtensionManager not found")
        }
    }
    
    /**
     * Initialize extension manager
     * @param configuredExtensionId The extension ID from configuration, if null will not set any default provider
     */
    fun initialize(configuredExtensionId: String? = null) {
        LOG.info("Initializing extension manager with configured extension: $configuredExtensionId")
        
        // Register all available extension providers
        registerExtensionProviders()
        
        if (configuredExtensionId != null) {
            // 如果配置了特定的extension，直接设置
            val provider = extensionProviders[configuredExtensionId]
            if (provider != null && provider.isAvailable(project)) {
                currentProvider = provider
                LOG.info("Set configured extension provider: $configuredExtensionId")
            } else {
                LOG.warn("Configured extension provider not available: $configuredExtensionId")
                // 不设置默认provider，让系统保持未初始化状态
                currentProvider = null
            }
        } else {
            // 只有在没有配置时才设置默认provider（可选）
            LOG.info("No extension configured, skipping default provider setup")
            // 注释掉自动设置默认provider的逻辑
            // setDefaultExtensionProvider()
        }
        
        LOG.info("Extension manager initialized")
    }
    
    /**
     * Initialize extension manager with default behavior (backward compatibility)
     */
    fun initialize() {
        initialize(null)
    }
    
    /**
     * Check if configuration is valid for this extension manager
     */
    fun isConfigurationValid(): Boolean {
        return currentProvider != null && currentProvider!!.isAvailable(project)
    }
    
    /**
     * Get configuration validation error message if any
     */
    fun getConfigurationError(): String? {
        return if (currentProvider == null) {
            "No extension provider set"
        } else if (!currentProvider!!.isAvailable(project)) {
            "Extension provider '${currentProvider!!.getExtensionId()}' is not available"
        } else null
    }
    
    /**
     * Check if extension manager is properly initialized with a valid provider
     */
    fun isProperlyInitialized(): Boolean {
        return currentProvider != null && currentProvider!!.isAvailable(project)
    }
    
    /**
     * Register extension providers
     */
    private fun registerExtensionProviders() {
        // Register Roo Code extension provider
        val rooProvider = RooExtensionProvider()
        registerExtensionProvider(rooProvider)

        // Register Cline AI extension provider
        val clineProvider = ClineExtensionProvider()
        registerExtensionProvider(clineProvider)

        // TODO: Register other extension providers here
        // val customProvider = com.sina.weibo.agent.extensions.custom.CustomExtensionProvider()
        // registerExtensionProvider(customProvider)
    }
    
    /**
     * Register an extension provider
     */
    fun registerExtensionProvider(provider: ExtensionProvider) {
        extensionProviders[provider.getExtensionId()] = provider
        LOG.info("Registered extension provider: ${provider.getExtensionId()}")
    }
    
    /**
     * Set default extension provider
     */
    private fun setDefaultExtensionProvider() {
        // Try to find available extension providers
        val availableProviders = extensionProviders.values.filter { it.isAvailable(project) }
        
        if (availableProviders.isNotEmpty()) {
            // Prefer roo-code as default provider
//             val rooProvider = availableProviders.find { it.getExtensionId() == "roo-code" }
            val rooProvider = availableProviders.find { it.getExtensionId() == "cline" }
            if (rooProvider != null) {
                currentProvider = rooProvider
                LOG.info("Set default extension provider: roo-code (preferred)")
            } else {
                // Fallback to first available provider
                currentProvider = availableProviders.first()
                LOG.info("Set default extension provider: ${currentProvider?.getExtensionId()}")
            }
        } else {
            LOG.warn("No available extension providers found")
        }
    }
    
    /**
     * Get current extension provider
     */
    fun getCurrentProvider(): ExtensionProvider? {
        return currentProvider
    }
    
    /**
     * Set current extension provider
     */
    fun setCurrentProvider(extensionId: String): Boolean {
        val provider = extensionProviders[extensionId]
        if (provider != null && provider.isAvailable(project)) {
            val oldProvider = currentProvider
            currentProvider = provider
            
            // Initialize new provider
            provider.initialize(project)
            
            // Update configuration
            try {
                val configManager = ExtensionConfigurationManager.getInstance(project)
                configManager.setCurrentExtensionId(extensionId)
            } catch (e: Exception) {
                LOG.warn("Failed to update configuration manager", e)
            }
                    try {
                        val buttonManager = DynamicButtonManager.getInstance(project)
                        buttonManager.setCurrentExtension(extensionId)
                    } catch (e: Exception) {
                        LOG.warn("Failed to update button configuration", e)
                    }
                    
                    // Notify listeners
                    try {
                        project.messageBus.syncPublisher(ExtensionChangeListener.EXTENSION_CHANGE_TOPIC)
                            .onExtensionChanged(extensionId)
                    } catch (e: Exception) {
                        LOG.warn("Failed to notify extension change listeners", e)
                    }
                    
                    // Update dynamic button manager
                    try {
                        val buttonManager = DynamicButtonManager.getInstance(project)
                        buttonManager.setCurrentExtension(extensionId)
                    } catch (e: Exception) {
                        LOG.warn("Failed to update button configuration", e)
                    }
                    
            LOG.info("Switched to extension provider: $extensionId (was: ${oldProvider?.getExtensionId()})")
            return true
        } else {
            LOG.warn("Extension provider not found or not available: $extensionId")
            return false
        }
    }
    
    /**
     * Switch extension provider with restart
     */
    fun switchExtensionProvider(extensionId: String, forceRestart: Boolean = false): CompletableFuture<Boolean> {
        val extensionSwitcher = ExtensionSwitcher.Companion.getInstance(project)
        return extensionSwitcher.switchExtension(extensionId, forceRestart)
    }
    
    /**
     * Get all available extension providers
     */
    fun getAvailableProviders(): List<ExtensionProvider> {
        return extensionProviders.values.filter { it.isAvailable(project) }
    }
    
    /**
     * Get all registered extension providers
     */
    fun getAllProviders(): List<ExtensionProvider> {
        return extensionProviders.values.toList()
    }
    
    /**
     * Get extension provider by ID
     */
    fun getProvider(extensionId: String): ExtensionProvider? {
        return extensionProviders[extensionId]
    }
    
    /**
     * Initialize current extension provider
     */
    fun initializeCurrentProvider() {
        currentProvider?.initialize(project)
    }
    
    /**
     * Dispose all extension providers
     */
    fun dispose() {
        LOG.info("Disposing extension manager")
        extensionProviders.values.forEach { it.dispose() }
        extensionProviders.clear()
        currentProvider = null
    }
} 