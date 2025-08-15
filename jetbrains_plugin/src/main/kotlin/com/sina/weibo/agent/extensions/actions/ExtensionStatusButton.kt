// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.extensions.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.actionSystem.ActionPlaces
import com.sina.weibo.agent.extensions.ExtensionManager
import com.sina.weibo.agent.extensions.ExtensionSwitcher
import com.sina.weibo.agent.extensions.ui.ExtensionSwitcherDialog
import com.intellij.openapi.actionSystem.ActionUpdateThread

/**
 * Extension status button
 * Shows current extension status and allows switching
 */
class ExtensionStatusButton : AnAction() {
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        // Check if switching is already in progress
        val extensionSwitcher = ExtensionSwitcher.getInstance(project)
        if (extensionSwitcher.isSwitching()) {
            Messages.showInfoMessage(
                "Extension switching is already in progress. Please wait for it to complete.",
                "Switch in Progress"
            )
            return
        }
        
        // Show extension switcher dialog
        val dialog = ExtensionSwitcherDialog(project)
        dialog.show()
    }
    
    override fun update(e: AnActionEvent) {
        val project = e.project
        val presentation = e.presentation
        
        if (project == null) {
            presentation.isEnabledAndVisible = false
            return
        }
        
        // Check if extensions are available
        val extensionManager = ExtensionManager.getInstance(project)
        val availableProviders = extensionManager.getAvailableProviders()
        
        if (availableProviders.size < 2) {
            presentation.isEnabledAndVisible = false
            return
        }
        
        // Update text and icon based on current extension
        val currentProvider = extensionManager.getCurrentProvider()
        val currentExtensionName = currentProvider?.getDisplayName() ?: "Unknown"
        
        // Different presentation for different places
        when (e.place) {
            ActionPlaces.TOOLBAR -> {
                presentation.text = currentExtensionName
                presentation.description = "Current: $currentExtensionName (Click to switch)"
                // Use different icons for different extensions
                presentation.icon = when (currentProvider?.getExtensionId()) {
                    "cline" -> com.intellij.icons.AllIcons.General.Modified
                    "roo-code" -> com.intellij.icons.AllIcons.General.Modified
                    else -> com.intellij.icons.AllIcons.General.Modified
                }
            }
            else -> {
                presentation.text = "Extension: $currentExtensionName"
                presentation.description = "Switch to a different extension provider"
                presentation.icon = com.intellij.icons.AllIcons.Actions.Refresh
            }
        }
        
        presentation.isEnabledAndVisible = true
        
        // Check if switching is in progress
        val extensionSwitcher = ExtensionSwitcher.getInstance(project)
        if (extensionSwitcher.isSwitching()) {
            presentation.text = "Switching..."
            presentation.description = "Extension switching in progress..."
            presentation.isEnabled = false
            presentation.icon = com.intellij.icons.AllIcons.Process.Step_passive
        }
    }
    
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}
