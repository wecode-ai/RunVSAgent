// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.extensions.plugin.cline

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import com.sina.weibo.agent.extensions.ui.contextmenu.ExtensionContextMenuProvider
import com.sina.weibo.agent.extensions.ui.contextmenu.ContextMenuConfiguration
import com.sina.weibo.agent.extensions.ui.contextmenu.ContextMenuActionType

/**
 * Cline extension context menu provider.
 * Provides context menu actions specific to Cline AI extension.
 * This includes Cline-specific functionality and commands.
 */
class ClineContextMenuProvider : ExtensionContextMenuProvider {
    
    override fun getExtensionId(): String = "cline"
    
    override fun getDisplayName(): String = "Cline AI"
    
    override fun getDescription(): String = "AI-powered coding assistant with Cline-specific context menu"
    
    override fun isAvailable(project: Project): Boolean {
        // Check if cline extension is available
        return true
    }
    
    override fun getContextMenuActions(project: Project): List<AnAction> {
        return listOf(
        )
    }
    
    override fun getContextMenuConfiguration(): ContextMenuConfiguration {
        return ClineContextMenuConfiguration()
    }
    
    /**
     * Cline context menu configuration - shows core actions only.
     */
    private class ClineContextMenuConfiguration : ContextMenuConfiguration {
        override fun isActionVisible(actionType: ContextMenuActionType): Boolean {
            return when (actionType) {
                ContextMenuActionType.EXPLAIN_CODE,
                ContextMenuActionType.FIX_CODE,
                ContextMenuActionType.IMPROVE_CODE,
                ContextMenuActionType.ADD_TO_CONTEXT,
                ContextMenuActionType.NEW_TASK -> true
                ContextMenuActionType.FIX_LOGIC -> false // Cline doesn't have separate logic fix
            }
        }
        
        override fun getVisibleActions(): List<ContextMenuActionType> {
            return listOf(
                ContextMenuActionType.EXPLAIN_CODE,
                ContextMenuActionType.FIX_CODE,
                ContextMenuActionType.IMPROVE_CODE,
                ContextMenuActionType.ADD_TO_CONTEXT,
                ContextMenuActionType.NEW_TASK
            )
        }
    }
}
