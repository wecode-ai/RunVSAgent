// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.extensions.roo.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.sina.weibo.agent.extensions.roo.ui.ExtensionTypeSelector
import com.sina.weibo.agent.extensions.roo.ExtensionConfiguration

/**
 * Action to show extension type selector dialog for Roo Code
 */
class ExtensionTypeSelectorAction : AnAction() {
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.getData(CommonDataKeys.PROJECT) ?: return
        
        val success = ExtensionTypeSelector.show(project)
        if (success) {
            // Optionally restart the extension or show a notification
            // For now, just log the change
            val extensionConfig = ExtensionConfiguration.getInstance(project)
            val currentType = extensionConfig.getCurrentExtensionType()
            com.intellij.openapi.diagnostic.Logger.getInstance(this::class.java)
                .info("Extension type changed to: ${currentType.code}")
        }
    }
    
    override fun update(e: AnActionEvent) {
        val project = e.getData(CommonDataKeys.PROJECT)
        e.presentation.isEnabledAndVisible = project != null
    }
} 