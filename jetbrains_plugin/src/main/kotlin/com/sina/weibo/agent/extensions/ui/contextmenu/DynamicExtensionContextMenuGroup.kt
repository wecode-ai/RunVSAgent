// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.extensions.ui.contextmenu

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.DumbAware

/**
 * Dynamic extension context menu action group.
 * This class manages the dynamic context menu actions that appear in the right-click menu
 * when text is selected, based on the current active extension.
 * 
 * Implements DumbAware to ensure the action works during indexing, and ActionUpdateThreadAware
 * to specify which thread should handle action updates.
 */
class DynamicExtensionContextMenuGroup : DefaultActionGroup(), DumbAware, ActionUpdateThreadAware {

    /**
     * Manager that provides the current extension's context menu actions.
     */
    private var contextMenuManager: DynamicContextMenuManager? = null

    /**
     * Updates the action group based on the current context and extension.
     * This method is called each time the menu needs to be displayed.
     *
     * @param e The action event containing context information
     */
    override fun update(e: AnActionEvent) {
        removeAll()

        // Check if there is an editor and selected text
        val editor = e.getData(CommonDataKeys.EDITOR)
        val hasSelection = editor?.selectionModel?.hasSelection() == true

        if (hasSelection) {
            loadDynamicContextMenuActions(e)
        }

        // Set the visibility of the action group
        e.presentation.isVisible = hasSelection
    }

    /**
     * Loads dynamic context menu actions into this action group based on the current extension.
     *
     * @param e The action event containing context information
     */
    private fun loadDynamicContextMenuActions(e: AnActionEvent) {
        val project = e.project ?: return
        
        // Get or initialize the context menu manager
        if (contextMenuManager == null) {
            try {
                contextMenuManager = DynamicContextMenuManager.getInstance(project)
                contextMenuManager?.initialize()
            } catch (e: Exception) {
                // If the manager is not available, fall back to default actions
                return
            }
        }

        // Get actions from the current extension
        val actions = contextMenuManager?.getContextMenuActions() ?: emptyList()
        actions.forEach { action ->
            add(action)
        }
    }

    /**
     * Specifies which thread should be used for updating this action.
     * EDT (Event Dispatch Thread) is used for UI-related operations.
     *
     * @return The thread to use for action updates
     */
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}
