// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.extensions

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
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
     */
    fun initialize() {
        LOG.info("Initializing extension manager")
        
        // Register all available extension providers
        registerExtensionProviders()
        
        // Set default extension provider
        setDefaultExtensionProvider()
        
        LOG.info("Extension manager initialized")
    }
    
    /**
     * Register extension providers
     */
    private fun registerExtensionProviders() {
        // Register Roo Code extension provider
        val rooProvider = com.sina.weibo.agent.extensions.roo.RooExtensionProvider()
        registerExtensionProvider(rooProvider)

        // Register Cline AI extension provider
        val clineProvider = com.sina.weibo.agent.extensions.cline.ClineExtensionProvider()
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
    fun switchExtensionProvider(extensionId: String, forceRestart: Boolean = false): java.util.concurrent.CompletableFuture<Boolean> {
        val extensionSwitcher = ExtensionSwitcher.getInstance(project)
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