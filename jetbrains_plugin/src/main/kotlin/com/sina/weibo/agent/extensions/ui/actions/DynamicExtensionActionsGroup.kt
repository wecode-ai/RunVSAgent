// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.extensions.ui.actions

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.DumbAware
import com.sina.weibo.agent.extensions.core.ExtensionManager
import com.sina.weibo.agent.extensions.config.ExtensionProvider
import com.sina.weibo.agent.extensions.common.ExtensionChangeListener
import com.sina.weibo.agent.extensions.plugin.cline.ClineButtonProvider
import com.sina.weibo.agent.extensions.plugin.roo.RooCodeButtonProvider
import com.sina.weibo.agent.extensions.ui.buttons.ExtensionButtonProvider

/**
 * Dynamic extension actions group that shows different buttons based on the current extension type.
 * This class dynamically generates buttons according to the current extension provider.
 */
class DynamicExtensionActionsGroup : DefaultActionGroup(), DumbAware, ActionUpdateThreadAware, ExtensionChangeListener {
    
    private val logger = Logger.getInstance(DynamicExtensionActionsGroup::class.java)
    
    /**
     * Updates the action group based on the current context and extension type.
     * This method is called each time the menu/toolbar needs to be displayed.
     *
     * @param e The action event containing context information
     */
    private var cachedButtonProvider: ExtensionButtonProvider? = null
    private var cachedExtensionId: String? = null
    private var cachedActions: List<AnAction>? = null
    
    override fun update(e: AnActionEvent) {
        val project = e.getData(CommonDataKeys.PROJECT)
        if (project == null) {
            e.presentation.isVisible = false
            return
        }
        
        try {
            val extensionManager = ExtensionManager.getInstance(project)
            val currentProvider = extensionManager.getCurrentProvider()
            
            if (currentProvider != null) {
                val extensionId = currentProvider.getExtensionId()
                
                // 检查是否需要更新缓存
                if (cachedExtensionId != extensionId || cachedActions == null) {
                    updateCachedActions(currentProvider, project)
                }
                
                // 使用缓存的actions
                if (cachedActions != null) {
                    removeAll()
                    cachedActions!!.forEach { action ->
                        add(action)
                    }
                    e.presentation.isVisible = true
                    logger.debug("Using cached actions for extension: $extensionId")
                }
            } else {
                e.presentation.isVisible = false
                logger.debug("No current extension provider, hiding dynamic actions")
            }
        } catch (exception: Exception) {
            logger.warn("Failed to load dynamic actions", exception)
            e.presentation.isVisible = false
        }
    }
    
    private fun updateCachedActions(provider: ExtensionProvider, project: Project) {
        val extensionId = provider.getExtensionId()
        
        // 创建新的ButtonProvider实例（仅在扩展类型改变时）
        val buttonProvider = when (extensionId) {
            "roo-code" -> RooCodeButtonProvider()
            "cline" -> ClineButtonProvider()
            else -> null
        }
        
        if (buttonProvider != null) {
            // 创建并缓存actions
            val actions = buttonProvider.getButtons(project)
            
            // 更新缓存
            cachedButtonProvider = buttonProvider
            cachedExtensionId = extensionId
            cachedActions = actions
            
            logger.debug("Updated cached actions for extension: $extensionId, count: ${actions.size}")
        }
    }
    
    /**
     * Loads dynamic actions into this action group based on the current extension provider.
     *
     * @param provider The current extension provider
     * @param project The current project
     */
    private fun loadDynamicActions(provider: ExtensionProvider, project: Project) {
        val extensionId = provider.getExtensionId()

        val buttonProvider = when (extensionId) {
            "roo-code" -> RooCodeButtonProvider()
            "cline" -> ClineButtonProvider()
            else -> null
        }
        // Create actions based on extension type
        val actions = buttonProvider?.getButtons(project) ?: emptyList()
        
        // Add all actions to the group
        actions.forEach { action ->
            add(action)
        }
        
        logger.debug("Added ${actions.size} actions for extension: $extensionId")
    }

    /**
     * Specifies which thread should be used for updating this action.
     * Returns BGT to ensure updates happen on the background thread.
     */
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
    
    /**
     * Called when the current extension changes.
     * This method is part of the ExtensionChangeListener interface.
     * 
     * @param newExtensionId The ID of the new extension
     */
    override fun onExtensionChanged(newExtensionId: String) {
        logger.info("Extension changed to: $newExtensionId, refreshing dynamic actions")
        
        // Note: The action group will be automatically refreshed when the UI is next displayed
        // No need to manually trigger an update here
    }
    
    // Note: getProjectFromContext method removed as it's no longer needed
}
