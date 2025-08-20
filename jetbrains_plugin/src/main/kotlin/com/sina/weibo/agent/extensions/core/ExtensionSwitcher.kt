// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.extensions.core

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.sina.weibo.agent.core.ExtensionUnixDomainSocketServer
import com.sina.weibo.agent.core.ISocketServer
import com.sina.weibo.agent.extensions.common.ExtensionChangeListener
import com.sina.weibo.agent.extensions.ui.buttons.DynamicButtonManager
import com.sina.weibo.agent.plugin.WecoderPluginService
import com.sina.weibo.agent.util.ExtensionUtils
import com.sina.weibo.agent.webview.WebViewManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.CompletableFuture

/**
 * Extension switcher service
 * Handles switching between different extension providers
 * Note: Switching now only saves configuration and takes effect on next startup
 */
@Service(Service.Level.PROJECT)
class ExtensionSwitcher(private val project: Project) {
    private val LOG = Logger.getInstance(ExtensionSwitcher::class.java)

    // Current switching state
    @Volatile
    private var isSwitching = false

    // Switching completion future
    private var switchingFuture: CompletableFuture<Boolean>? = null

    // Coroutine scope for switching operations
    private val switchingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        fun getInstance(project: Project): ExtensionSwitcher {
            return project.getService(ExtensionSwitcher::class.java)
                ?: error("ExtensionSwitcher not found")
        }
    }

    /**
     * Check if required services are available
     */
    private fun checkServicesAvailability(): Boolean {
        return try {
            val pluginService = project.getService(WecoderPluginService::class.java)
            if (pluginService == null) {
                LOG.error("WecoderPluginService not available")
                return false
            }

            // Check if process manager is available
            pluginService.getProcessManager()
            // Note: getProcessManager() should never return null based on the interface

            // Check if socket server is available
            pluginService.getSocketServer()
            // Note: getSocketServer() should never return null based on the interface

            // Check if UDS server is available (for non-Windows)
//            if (!SystemInfo.isWindows) {
//                val udsServer = project.getService(com.sina.weibo.agent.core.ExtensionUnixDomainSocketServer::class.java)
//                if (udsServer == null) {
//                    LOG.error("ExtensionUnixDomainSocketServer not available")
//                    return false
//                }
//            }

            true
        } catch (e: Exception) {
            LOG.error("Error checking services availability", e)
            false
        }
    }

    /**
     * Switch to a different extension provider
     * @param extensionId Target extension ID
     * @param forceRestart Whether to force restart the extension process (ignored in new mode)
     * @return Future that completes when switching is done
     */
    fun switchExtension(extensionId: String, forceRestart: Boolean = false): CompletableFuture<Boolean> {
        if (isSwitching) {
            LOG.warn("Extension switching already in progress")
            return CompletableFuture.completedFuture(false)
        }

        val extensionManager = ExtensionManager.getInstance(project)
        val targetProvider = extensionManager.getProvider(extensionId)

        if (targetProvider == null) {
            LOG.error("Extension provider not found: $extensionId")
            return CompletableFuture.completedFuture(false)
        }

        if (!targetProvider.isAvailable(project)) {
            LOG.error("Extension provider not available: $extensionId")
            return CompletableFuture.completedFuture(false)
        }

        val currentProvider = extensionManager.getCurrentProvider()
        if (currentProvider?.getExtensionId() == extensionId) {
            LOG.info("Already using extension provider: $extensionId")
            return CompletableFuture.completedFuture(true)
        }

        LOG.info("Starting extension switch from ${currentProvider?.getExtensionId()} to $extensionId (will take effect on next startup)")

        // Check if required services are available
        if (!checkServicesAvailability()) {
            LOG.error("Required services not available, cannot perform extension switch")
            return CompletableFuture.completedFuture(false)
        }

        isSwitching = true
        switchingFuture = CompletableFuture()

        // Perform switching in background
        switchingScope.launch {
            try {
                val success = performExtensionSwitch(extensionId, forceRestart)
                switchingFuture?.complete(success)
            } catch (e: Exception) {
                LOG.error("Error during extension switching", e)
                switchingFuture?.completeExceptionally(e)
            } finally {
                isSwitching = false
            }
        }

        return switchingFuture!!
    }

    /**
     * Perform the actual extension switching
     * Note: In new mode, this only saves configuration and updates UI state
     */
    private suspend fun performExtensionSwitch(extensionId: String, forceRestart: Boolean): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Step 1: Update extension manager (this will save configuration)
                updateExtensionManager(extensionId)

                // Step 2: Update button configuration
                updateButtonConfiguration(extensionId)

                // Step 3: Notify UI components
                notifyExtensionChanged(extensionId)

                LOG.info("Extension switching configuration saved successfully: $extensionId (will take effect on next startup)")
                true
            } catch (e: Exception) {
                LOG.error("Failed to switch extension: ${e.message}", e)
                false
            }
        }
    }

    /**
     * Update extension manager with new provider
     * This will save the configuration but not restart the process
     */
    private suspend fun updateExtensionManager(extensionId: String) {
        withContext(Dispatchers.Main) {
            val extensionManager = ExtensionManager.getInstance(project)

            // Set new extension provider (this will save configuration)
            val success = extensionManager.setCurrentProvider(extensionId)
            if (!success) {
                throw IllegalStateException("Failed to set extension provider: $extensionId")
            }

            LOG.info("Extension manager updated with new provider: $extensionId (configuration saved)")
        }
    }

    /**
     * Update button configuration for the new extension
     */
    private suspend fun updateButtonConfiguration(extensionId: String) {
        withContext(Dispatchers.Main) {
            try {
                val buttonManager = DynamicButtonManager.Companion.getInstance(project)
                buttonManager.setCurrentExtension(extensionId)
                LOG.info("Button configuration updated for extension: $extensionId")
            } catch (e: Exception) {
                LOG.warn("Failed to update button configuration", e)
            }
        }
    }

    /**
     * Notify UI components about extension change
     */
    private suspend fun notifyExtensionChanged(extensionId: String) {
        withContext(Dispatchers.Main) {
            // Notify WebView manager if available
            try {
                val webViewManager = project.getService(WebViewManager::class.java)
                // Note: WebViewManager may not have onExtensionChanged method yet
                // This will be implemented when WebViewManager supports extension changes
                // For now, we just check if it's available but don't use it
                if (webViewManager != null) {
                    LOG.debug("WebViewManager is available but extension change notification not implemented yet")
                }
            } catch (e: Exception) {
                // WebViewManager not available or doesn't support extension changes yet
                LOG.debug("WebViewManager not available: ${e.message}")
            }

            // Notify other components
            project.messageBus.syncPublisher(ExtensionChangeListener.EXTENSION_CHANGE_TOPIC)
                .onExtensionChanged(extensionId)
        }
    }

    /**
     * Check if switching is in progress
     */
    fun isSwitching(): Boolean = isSwitching

    /**
     * Wait for current switching to complete
     */
    fun waitForSwitching(): CompletableFuture<Boolean>? = switchingFuture

    /**
     * Cancel current switching operation
     */
    fun cancelSwitching() {
        if (isSwitching) {
            switchingScope.cancel("Extension switching cancelled")
            isSwitching = false
            switchingFuture?.cancel(true)
            LOG.info("Extension switching cancelled")
        }
    }

    /**
     * Dispose resources
     */
    fun dispose() {
        switchingScope.cancel()
        switchingFuture?.cancel(true)
    }
}