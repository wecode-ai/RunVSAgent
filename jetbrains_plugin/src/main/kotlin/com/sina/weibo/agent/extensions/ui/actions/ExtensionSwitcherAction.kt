// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.extensions.ui.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.actionSystem.ActionPlaces
import com.sina.weibo.agent.extensions.core.ExtensionManager
import com.sina.weibo.agent.extensions.core.ExtensionSwitcher
import com.sina.weibo.agent.extensions.ui.ExtensionSwitcherDialog

/**
 * Extension switcher action
 * Provides quick access to extension switching functionality
 */
class ExtensionSwitcherAction : AnAction() {
    
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
        
        // Since we now support uploading VSIX files after startup,
        // we show the button even if there's only one extension
        // The user can still switch to upload VSIX for the same extension
        if (availableProviders.isEmpty()) {
            presentation.isEnabledAndVisible = false
            presentation.text = "No Extensions Available"
            return
        }
        
        // Update text to show current extension
        val currentProvider = extensionManager.getCurrentProvider()
        val currentExtensionName = currentProvider?.getDisplayName() ?: "Unknown"
        
        // Different text for different places
        when (e.place) {
            ActionPlaces.TOOLBAR -> {
                presentation.text = "Switch ($currentExtensionName)"
                presentation.description = "Switch to a different extension provider or upload VSIX"
            }
            ActionPlaces.MAIN_MENU -> {
                presentation.text = "Switch Extension Provider"
                presentation.description = "Switch to a different extension provider or upload VSIX"
            }
            else -> {
                presentation.text = "Switch Extension ($currentExtensionName)"
                presentation.description = "Switch to a different extension provider or upload VSIX"
            }
        }
        
        presentation.isEnabledAndVisible = true
        
        // Check if switching is in progress
        val extensionSwitcher = ExtensionSwitcher.getInstance(project)
        if (extensionSwitcher.isSwitching()) {
            presentation.text = "Switching..."
            presentation.description = "Extension switching in progress..."
            presentation.isEnabled = false
        }
    }
}
