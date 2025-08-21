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

/**
 * Dynamic context menu manager that controls which context menu actions are available
 * based on the current extension type.
 * This manager works in conjunction with DynamicExtensionContextMenuGroup to provide
 * dynamic context menu functionality.
 */
@Service(Service.Level.PROJECT)
class DynamicContextMenuManager(private val project: Project) {
    
    private val logger = Logger.getInstance(DynamicContextMenuManager::class.java)
    
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
            val extensionManager = ExtensionManager.Companion.getInstance(project)
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
        return currentExtensionId
    }
    
    /**
     * Get context menu configuration for the current extension
     */
    fun getContextMenuConfiguration(): ContextMenuConfiguration {
        val contextMenuProvider = getContextMenuProvider(currentExtensionId)
        return contextMenuProvider?.getContextMenuConfiguration() ?: DefaultContextMenuConfiguration()
    }
    
    /**
     * Get context menu actions for the current extension
     */
    fun getContextMenuActions(): List<com.intellij.openapi.actionSystem.AnAction> {
        val contextMenuProvider = getContextMenuProvider(currentExtensionId)
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
            // Get the action manager
            val actionManager = com.intellij.openapi.actionSystem.ActionManager.getInstance()
            
            // Refresh the dynamic context menu actions group by invalidating the action
            // This will trigger the UI to refresh without directly calling @ApiStatus.OverrideOnly methods
            val dynamicGroup = actionManager.getAction("RunVSAgent.DynamicExtensionContextMenu")
            dynamicGroup?.let { group ->
                // Use the proper IntelliJ Platform mechanism to refresh the action
                // Instead of calling update() directly, we invalidate the action
                actionManager.invalidateAction(group.id)
            }
            
            logger.debug("Context menus refreshed for extension: $currentExtensionId")
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
