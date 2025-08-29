// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.extensions.plugin.kilo

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import com.sina.weibo.agent.extensions.ui.contextmenu.ExtensionContextMenuProvider
import com.sina.weibo.agent.extensions.ui.contextmenu.ContextMenuConfiguration
import com.sina.weibo.agent.extensions.ui.contextmenu.ContextMenuActionType

/**
 * Kilo Code extension context menu provider.
 * Provides context menu actions specific to Kilo Code extension.
 * This includes Kilo Code-specific functionality and commands.
 */
class KiloCodeContextMenuProvider : ExtensionContextMenuProvider {
    
    override fun getExtensionId(): String = "kilo-code"
    
    override fun getDisplayName(): String = "Kilo Code"
    
    override fun getDescription(): String = "AI-powered code assistant with advanced capabilities and context menu"
    
    override fun isAvailable(project: Project): Boolean {
        // Check if kilo-code extension is available
        return true
    }
    
    override fun getContextMenuActions(project: Project): List<AnAction> {
        return listOf(
        )
    }
    
    override fun getContextMenuConfiguration(): ContextMenuConfiguration {
        return KiloCodeContextMenuConfiguration()
    }
    
    /**
     * Kilo Code context menu configuration - shows all available actions.
     */
    private class KiloCodeContextMenuConfiguration : ContextMenuConfiguration {
        override fun isActionVisible(actionType: ContextMenuActionType): Boolean {
            return when (actionType) {
                ContextMenuActionType.EXPLAIN_CODE,
                ContextMenuActionType.FIX_CODE,
                ContextMenuActionType.FIX_LOGIC,
                ContextMenuActionType.IMPROVE_CODE,
                ContextMenuActionType.ADD_TO_CONTEXT,
                ContextMenuActionType.NEW_TASK -> true
            }
        }
        
        override fun getVisibleActions(): List<ContextMenuActionType> {
            return listOf(
                ContextMenuActionType.EXPLAIN_CODE,
                ContextMenuActionType.FIX_CODE,
                ContextMenuActionType.FIX_LOGIC,
                ContextMenuActionType.IMPROVE_CODE,
                ContextMenuActionType.ADD_TO_CONTEXT,
                ContextMenuActionType.NEW_TASK
            )
        }
    }
}