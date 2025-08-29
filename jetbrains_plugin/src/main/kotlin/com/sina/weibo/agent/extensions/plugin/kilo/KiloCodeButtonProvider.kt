// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.extensions.plugin.kilo

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ui.Messages
import com.sina.weibo.agent.actions.*
import com.sina.weibo.agent.extensions.ui.buttons.ExtensionButtonProvider
import com.sina.weibo.agent.extensions.ui.buttons.ButtonType
import com.sina.weibo.agent.extensions.ui.buttons.ButtonConfiguration
import com.sina.weibo.agent.webview.WebViewManager

/**
 * Kilo Code extension button provider.
 * Provides button configuration specific to Kilo Code extension.
 */
class KiloCodeButtonProvider : ExtensionButtonProvider {
    
    override fun getExtensionId(): String = "kilo-code"
    
    override fun getDisplayName(): String = "Kilo Code"
    
    override fun getDescription(): String = "AI-powered code assistant with advanced capabilities"
    
    override fun isAvailable(project: Project): Boolean {
        // Check if kilo-code extension is available
        // This could include checking for API keys, network connectivity, etc.
        return true
    }
    
    override fun getButtons(project: Project): List<AnAction> {
        // Note: project parameter kept for future extensibility
        return listOf(
            createPlusButton(),
            createPromptsButton(),
            createMcpButton(),
            createHistoryButton(),
            createMarketplaceButton(),
            createSettingsButton()
        )
    }
    
    /**
     * Create Plus button with icon and description
     */
    private fun createPlusButton(): AnAction {
        return object : AnAction() {
            init {
                templatePresentation.icon = AllIcons.General.Add
                templatePresentation.text = "New Task"
                templatePresentation.description = "New task"
            }
            
            override fun actionPerformed(e: AnActionEvent) {
                val logger = Logger.getInstance(this::class.java)
                logger.info("🔍 Kilo Code Plus button clicked, command: kilo-code.plusButtonClicked")
                logger.info("🔍 Project: ${e.project?.name}")
                
                // Check WebView status before executing command
                val project = e.project
                if (project != null) {
                    try {
                        val webViewManager = project.getService(WebViewManager::class.java)
                        if (webViewManager != null) {
                            val latestWebView = webViewManager.getLatestWebView()

                            if (latestWebView != null) {
                                logger.info("✅ WebView instances available, executing command...")
                                executeCommand("kilo-code.plusButtonClicked", project, hasArgs = false)
                                logger.info("✅ Command executed successfully")
                            } else {
                                logger.warn("⚠️ No WebView instances available")
                                // Show user-friendly message
                                Messages.showWarningDialog(
                                    project,
                                    "No active WebView found. Please ensure the Kilo Code extension is properly initialized.",
                                    "WebView Not Available"
                                )
                            }
                        } else {
                            logger.warn("⚠️ WebView Manager not available")
                        }
                    } catch (e: Exception) {
                        logger.error("❌ Error checking WebView status", e)
                    }
                } else {
                    logger.warn("⚠️ Project is null")
                }
            }
        }
    }

    /**
     * Create Prompts button with icon and description
     */
    private fun createPromptsButton(): AnAction {
        return object : AnAction() {
            init {
                templatePresentation.icon = AllIcons.General.Information
                templatePresentation.text = "Prompts"
                templatePresentation.description = "Prompts"
            }

            override fun actionPerformed(e: AnActionEvent) {
                Logger.getInstance(this::class.java).info("Prompts button clicked")
                executeCommand("kilo-code.promptsButtonClicked", e.project, hasArgs = false)
            }
        }
    }

    /**
     * kilo-code.mcpButtonClicked
     */
    private fun createMcpButton(): AnAction {
        return object : AnAction() {
            init {
                templatePresentation.icon = AllIcons.Webreferences.Server
                templatePresentation.text = "MCP"
                templatePresentation.description = "MCP"
            }

            override fun actionPerformed(e: AnActionEvent) {
                Logger.getInstance(this::class.java).info("MCP button clicked")
                executeCommand("kilo-code.mcpButtonClicked", e.project, hasArgs = false)
            }
        }
    }

    /**
     * Create History button with icon and description
     */
    private fun createHistoryButton(): AnAction {
        return object : AnAction() {
            init {
                templatePresentation.icon = AllIcons.Vcs.History
                templatePresentation.text = "History"
                templatePresentation.description = "History"
            }
            
            override fun actionPerformed(e: AnActionEvent) {
                Logger.getInstance(this::class.java).info("History button clicked")
                executeCommand("kilo-code.historyButtonClicked", e.project, hasArgs = false)
            }
        }
    }

    /**
     * Create Marketplace button with icon and description
     */
    private fun createMarketplaceButton(): AnAction {
        return object : AnAction() {
            init {
                templatePresentation.icon = AllIcons.Nodes.ModuleGroup
                templatePresentation.text = "Marketplace"
                templatePresentation.description = "Marketplace"
            }
            
            override fun actionPerformed(e: AnActionEvent) {
                Logger.getInstance(this::class.java).info("Marketplace button clicked")
                executeCommand("kilo-code.marketplaceButtonClicked", e.project, hasArgs = false)
            }
        }
    }

    /**
     * Create Settings button with icon and description
     */
    private fun createSettingsButton(): AnAction {
        return object : AnAction() {
            init {
                templatePresentation.icon = AllIcons.General.Settings
                templatePresentation.text = "Settings"
                templatePresentation.description = "Settings"
            }
            
            override fun actionPerformed(e: AnActionEvent) {
                Logger.getInstance(this::class.java).info("Settings button clicked")
                executeCommand("kilo-code.settingsButtonClicked", e.project, hasArgs = false)
            }
        }
    }
    
    override fun getButtonConfiguration(): ButtonConfiguration {
        return KiloCodeButtonConfiguration()
    }
    
    /**
     * Kilo Code button configuration - shows all buttons (full-featured).
     */
    private class KiloCodeButtonConfiguration : ButtonConfiguration {
        override fun isButtonVisible(buttonType: ButtonType): Boolean {
            return true // All buttons are visible for Kilo Code
        }
        
        override fun getVisibleButtons(): List<ButtonType> {
            return ButtonType.values().toList()
        }
    }
}