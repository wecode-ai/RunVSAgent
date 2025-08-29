// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.extensions.ui.contextmenu

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.sina.weibo.agent.extensions.core.ExtensionManager
import com.sina.weibo.agent.extensions.plugin.cline.ClineContextMenuProvider
import com.sina.weibo.agent.extensions.plugin.roo.RooCodeContextMenuProvider
import com.sina.weibo.agent.extensions.plugin.kilo.KiloCodeContextMenuProvider

/**
 * Dynamic context menu manager that controls which context menu actions are available
 * based on the current extension type.
 * This manager works in conjunction with DynamicExtensionContextMenuGroup to provide
 * dynamic context menu functionality.
 */
@Service(Service.Level.PROJECT)
class DynamicContextMenuManager(private val project: Project) {
    
    private val logger = Logger.getInstance(DynamicContextMenuManager::class.java)
    private val extensionManager = ExtensionManager.Companion.getInstance(project)

    // Current extension ID
    @Volatile
    private var currentExtensionId: String? = null
    
    companion object {
        /**
         * Get dynamic context menu manager instance
         */
        fun getInstance(project: Project): DynamicContextMenuManager {
            return project.getService(DynamicContextMenuManager::class.java)
                ?: error("DynamicContextMenuManager not found")
        }
    }
    
    /**
     * Initialize the dynamic context menu manager
     */
    fun initialize() {
        logger.info("Initializing dynamic context menu manager")
        
        // Get current extension from extension manager
        try {
            val currentProvider = extensionManager.getCurrentProvider()
            currentExtensionId = currentProvider?.getExtensionId()
            logger.info("Dynamic context menu manager initialized with extension: $currentExtensionId")
        } catch (e: Exception) {
            logger.warn("Failed to initialize dynamic context menu manager", e)
        }
    }
    
    /**
     * Set the current extension and update context menu configuration
     */
    fun setCurrentExtension(extensionId: String) {
        logger.info("Setting current extension to: $extensionId")
        currentExtensionId = extensionId
        
        // Refresh all context menus to reflect the change
        refreshContextMenus()
    }
    
    /**
     * Get the current extension ID
     */
    fun getCurrentExtensionId(): String? {
        return extensionManager.getCurrentProvider()?.getExtensionId()
    }
    
    /**
     * Get context menu configuration for the current extension
     */
    fun getContextMenuConfiguration(): ContextMenuConfiguration {
        val contextMenuProvider = getContextMenuProvider(getCurrentExtensionId())
        return contextMenuProvider?.getContextMenuConfiguration() ?: DefaultContextMenuConfiguration()
    }
    
    /**
     * Get context menu actions for the current extension
     */
    fun getContextMenuActions(): List<com.intellij.openapi.actionSystem.AnAction> {
        val contextMenuProvider = getContextMenuProvider(getCurrentExtensionId())
        return contextMenuProvider?.getContextMenuActions(project) ?: emptyList()
    }
    
    /**
     * Get context menu provider for the specified extension.
     *
     * @param extensionId The extension ID
     * @return Context menu provider instance or null if not found
     */
    private fun getContextMenuProvider(extensionId: String?): ExtensionContextMenuProvider? {
        if (extensionId == null) return null
        
        return when (extensionId) {
            "roo-code" -> RooCodeContextMenuProvider()
            "cline" -> ClineContextMenuProvider()
            "kilo-code" -> KiloCodeContextMenuProvider()
            // TODO: Add other context menu providers as they are implemented
            // "copilot" -> CopilotContextMenuProvider()
            // "claude" -> ClaudeContextMenuProvider()
            else -> null
        }
    }
    
    /**
     * Check if a specific context menu action should be visible for the current extension
     */
    fun isActionVisible(actionType: ContextMenuActionType): Boolean {
        val config = getContextMenuConfiguration()
        return config.isActionVisible(actionType)
    }
    
    /**
     * Refresh all context menus to reflect current configuration
     */
    private fun refreshContextMenus() {
        try {
            // Use IntelliJ Platform's proper mechanism to refresh UI on EDT thread
            // This avoids calling @ApiStatus.OverrideOnly methods directly
            com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                try {
                    // Get the action manager
                    val actionManager = com.intellij.openapi.actionSystem.ActionManager.getInstance()
                    
                    // Get the dynamic context menu actions group
                    val dynamicGroup = actionManager.getAction("RunVSAgent.DynamicExtensionContextMenu")
                    dynamicGroup?.let { group ->
                        // Trigger UI refresh by notifying the platform
                        // The platform will automatically call the appropriate update methods
                        logger.debug("Triggering UI refresh for dynamic context menu group")
                    }
                    
                    logger.debug("Context menus refresh scheduled for extension: $currentExtensionId")
                } catch (e: Exception) {
                    logger.warn("Failed to schedule context menu refresh", e)
                }
            }
        } catch (e: Exception) {
            logger.warn("Failed to refresh context menus", e)
        }
    }
    
    /**
     * Dispose the dynamic context menu manager
     */
    fun dispose() {
        logger.info("Disposing dynamic context menu manager")
        currentExtensionId = null
    }
}

/**
 * Default context menu configuration - shows minimal actions
 */
class DefaultContextMenuConfiguration : ContextMenuConfiguration {
    override fun isActionVisible(actionType: ContextMenuActionType): Boolean {
        return when (actionType) {
            ContextMenuActionType.EXPLAIN_CODE,
            ContextMenuActionType.ADD_TO_CONTEXT -> true
            ContextMenuActionType.FIX_CODE,
            ContextMenuActionType.FIX_LOGIC,
            ContextMenuActionType.IMPROVE_CODE,
            ContextMenuActionType.NEW_TASK -> false
        }
    }
    
    override fun getVisibleActions(): List<ContextMenuActionType> {
        return listOf(
            ContextMenuActionType.EXPLAIN_CODE,
            ContextMenuActionType.ADD_TO_CONTEXT
        )
    }
}
