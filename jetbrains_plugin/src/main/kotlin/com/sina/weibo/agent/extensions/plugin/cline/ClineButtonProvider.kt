// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.extensions.plugin.cline

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
 * Cline extension button provider.
 * Provides button configuration specific to Cline AI extension.
 */
class ClineButtonProvider : ExtensionButtonProvider {
    
    override fun getExtensionId(): String = "cline"
    
    override fun getDisplayName(): String = "Cline AI"
    
    override fun getDescription(): String = "AI-powered code completion and chat using Cline AI"
    
    override fun isAvailable(project: Project): Boolean {
        // Check if cline extension is available
        // This could include checking for API keys, network connectivity, etc.
        return true
    }
    
    override fun getButtons(project: Project): List<AnAction> {
        // Note: project parameter kept for future extensibility
        return listOf(
            createPlusButton(),
            createMcpButton(),
            createHistoryButton(),
            createAccountButton(),
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
                logger.info("🔍 Cline Plus button clicked, command: cline.plusButtonClicked")
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
                                executeCommand("cline.plusButtonClicked", project, hasArgs = false)
                                logger.info("✅ Command executed successfully")
                            } else {
                                logger.warn("⚠️ No WebView instances available")
                                // Show user-friendly message
                                Messages.showWarningDialog(
                                    project,
                                    "No active WebView found. Please ensure the Cline extension is properly initialized.",
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
     * cline.mcpButtonClicked
     */
    private fun createMcpButton(): AnAction {
        return object : AnAction() {
            init {
                templatePresentation.icon = AllIcons.Webreferences.Server
                templatePresentation.text = "MCP"
                templatePresentation.description = "MCP"
            }

            override fun actionPerformed(e: AnActionEvent) {
                Logger.getInstance(this::class.java).info("Mcp button clicked")
                executeCommand("cline.mcpButtonClicked", e.project, hasArgs = false)
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
                executeCommand("cline.historyButtonClicked", e.project, hasArgs = false)
            }
        }
    }

    /**
     * cline.accountButtonClicked
     */
    private fun createAccountButton(): AnAction {
        return object : AnAction() {
            init {
                templatePresentation.icon = AllIcons.General.User
                templatePresentation.text = "Account"
                templatePresentation.description = "Account"
            }

            override fun actionPerformed(e: AnActionEvent) {
                Logger.getInstance(this::class.java).info("Account button clicked")
                executeCommand("cline.accountButtonClicked", e.project, hasArgs = false)
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
                templatePresentation.description = "Setting"
            }
            
            override fun actionPerformed(e: AnActionEvent) {
                Logger.getInstance(this::class.java).info("Settings button clicked")
                executeCommand("cline.settingsButtonClicked", e.project, hasArgs = false)
            }
        }
    }
    
    override fun getButtonConfiguration(): ButtonConfiguration {
        return ClineButtonConfiguration()
    }
    
    /**
     * Cline AI button configuration - shows core buttons only.
     */
    private class ClineButtonConfiguration : ButtonConfiguration {
        override fun isButtonVisible(buttonType: ButtonType): Boolean {
            return when (buttonType) {
                ButtonType.PLUS,
                ButtonType.PROMPTS,
                ButtonType.HISTORY,
                ButtonType.SETTINGS -> true
                ButtonType.MCP,
                ButtonType.MARKETPLACE -> false
            }
        }
        
        override fun getVisibleButtons(): List<ButtonType> {
            return listOf(
                ButtonType.PLUS,
                ButtonType.PROMPTS,
                ButtonType.HISTORY,
                ButtonType.SETTINGS
            )
        }
    }
}
