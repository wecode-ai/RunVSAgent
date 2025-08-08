// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.extensions.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.sina.weibo.agent.extensions.ui.ExtensionSelector
import com.sina.weibo.agent.extensions.ExtensionManager

/**
 * Action to show extension selector dialog
 */
class ExtensionSelectorAction : AnAction() {
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.getData(CommonDataKeys.PROJECT) ?: return
        
        val success = ExtensionSelector.show(project)
        if (success) {
            // Optionally restart the extension or show a notification
            val extensionManager = ExtensionManager.getInstance(project)
            val currentProvider = extensionManager.getCurrentProvider()
            com.intellij.openapi.diagnostic.Logger.getInstance(this::class.java)
                .info("Extension changed to: ${currentProvider?.getExtensionId()}")
        }
    }
    
    override fun update(e: AnActionEvent) {
        val project = e.getData(CommonDataKeys.PROJECT)
        e.presentation.isEnabledAndVisible = project != null
    }
} 