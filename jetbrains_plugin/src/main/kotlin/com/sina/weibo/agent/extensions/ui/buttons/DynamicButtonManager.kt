// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.extensions.ui.buttons

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.sina.weibo.agent.extensions.core.ExtensionManager
import com.sina.weibo.agent.extensions.plugin.cline.ClineButtonProvider
import com.sina.weibo.agent.extensions.plugin.roo.RooCodeButtonProvider

/**
 * Dynamic button manager that controls which buttons are visible based on the current extension type.
 * This manager works in conjunction with DynamicExtensionActionsGroup to provide dynamic button functionality.
 */
@Service(Service.Level.PROJECT)
class DynamicButtonManager(private val project: Project) {
    
    private val logger = Logger.getInstance(DynamicButtonManager::class.java)
    
    // Current extension ID
    @Volatile
    private var currentExtensionId: String? = null
    
    companion object {
        /**
         * Get dynamic button manager instance
         */
        fun getInstance(project: Project): DynamicButtonManager {
            return project.getService(DynamicButtonManager::class.java)
                ?: error("DynamicButtonManager not found")
        }
    }
    
    /**
     * Initialize the dynamic button manager
     */
    fun initialize() {
        logger.info("Initializing dynamic button manager")
        
        // Get current extension from extension manager
        try {
            val extensionManager = ExtensionManager.Companion.getInstance(project)
            val currentProvider = extensionManager.getCurrentProvider()
            currentExtensionId = currentProvider?.getExtensionId()
            logger.info("Dynamic button manager initialized with extension: $currentExtensionId")
        } catch (e: Exception) {
            logger.warn("Failed to initialize dynamic button manager", e)
        }
    }
    
    /**
     * Set the current extension and update button configuration
     */
    fun setCurrentExtension(extensionId: String) {
        logger.info("Setting current extension to: $extensionId")
        currentExtensionId = extensionId
        
        // Refresh all action toolbars to reflect the change
        refreshActionToolbars()
    }
    
    /**
     * Get the current extension ID
     */
    fun getCurrentExtensionId(): String? {
        return currentExtensionId
    }
    
    /**
     * Get button configuration for the current extension
     */
    fun getButtonConfiguration(): ButtonConfiguration {
        val buttonProvider = getButtonProvider(currentExtensionId)
        return buttonProvider?.getButtonConfiguration() ?: DefaultButtonConfiguration()
    }
    
    /**
     * Get button provider for the specified extension.
     *
     * @param extensionId The extension ID
     * @return Button provider instance or null if not found
     */
    private fun getButtonProvider(extensionId: String?): ExtensionButtonProvider? {
        if (extensionId == null) return null
        
        return when (extensionId) {
            "roo-code" -> RooCodeButtonProvider()
            "cline" -> ClineButtonProvider()
            // TODO: Add other button providers as they are implemented
            // "copilot" -> CopilotButtonProvider()
            // "claude" -> ClaudeButtonProvider()
            else -> null
        }
    }
    
    /**
     * Check if a specific button should be visible for the current extension
     */
    fun isButtonVisible(buttonType: ButtonType): Boolean {
        val config = getButtonConfiguration()
        return config.isButtonVisible(buttonType)
    }
    
    /**
     * Refresh all action toolbars to reflect current button configuration
     */
    private fun refreshActionToolbars() {
        try {
            // Get the action manager
            val actionManager = ActionManager.getInstance()
            
            // Refresh the dynamic actions group
            val dynamicGroup = actionManager.getAction("RunVSAgent.DynamicExtensionActions")
            dynamicGroup?.let { group ->
                // Create a dummy event for updating
                val dummyEvent = AnActionEvent.createFromAnAction(
                    group,
                    null,
                    "DynamicButtonManager",
                    DataManager.getInstance().dataContext
                )
                group.update(dummyEvent)
            }
            
            logger.debug("Action toolbars refreshed for extension: $currentExtensionId")
        } catch (e: Exception) {
            logger.warn("Failed to refresh action toolbars", e)
        }
    }
    
    /**
     * Dispose the dynamic button manager
     */
    fun dispose() {
        logger.info("Disposing dynamic button manager")
        currentExtensionId = null
    }
}

/**
 * Button types that can be configured
 */
enum class ButtonType {
    PLUS,
    PROMPTS,
    MCP,
    HISTORY,
    MARKETPLACE,
    SETTINGS
}

/**
 * Button configuration interface
 */
interface ButtonConfiguration {
    fun isButtonVisible(buttonType: ButtonType): Boolean
    fun getVisibleButtons(): List<ButtonType>
}

// Note: Button configurations are now provided by individual ExtensionButtonProvider implementations

/**
 * Default button configuration - shows minimal buttons
 */
class DefaultButtonConfiguration : ButtonConfiguration {
    override fun isButtonVisible(buttonType: ButtonType): Boolean {
        return when (buttonType) {
            ButtonType.PLUS,
            ButtonType.PROMPTS,
            ButtonType.SETTINGS -> true
            ButtonType.MCP,
            ButtonType.HISTORY,
            ButtonType.MARKETPLACE -> false
        }
    }
    
    override fun getVisibleButtons(): List<ButtonType> {
        return listOf(
            ButtonType.PLUS,
            ButtonType.PROMPTS,
            ButtonType.SETTINGS
        )
    }
}
