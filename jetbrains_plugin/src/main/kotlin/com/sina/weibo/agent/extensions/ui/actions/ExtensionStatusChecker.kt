// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.extensions.ui.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.sina.weibo.agent.extensions.core.ExtensionManager
import com.sina.weibo.agent.core.PluginContext
import com.sina.weibo.agent.core.ServiceProxyRegistry
import com.sina.weibo.agent.webview.WebViewManager

/**
 * Action to check extension status and diagnose issues
 */
class ExtensionStatusChecker : AnAction("Check Extension Status") {
    
    private val logger = Logger.getInstance(ExtensionStatusChecker::class.java)
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.getData(CommonDataKeys.PROJECT) ?: return
        
        val status = checkExtensionStatus(project)
        showStatusDialog(status)
    }
    
    private fun checkExtensionStatus(project: Project): String {
        val sb = StringBuilder()
        sb.appendLine("🔍 Extension Status Check")
        sb.appendLine("=".repeat(50))
        
        // Check Extension Manager
        try {
            val extensionManager = ExtensionManager.getInstance(project)
            val currentProvider = extensionManager.getCurrentProvider()
            sb.appendLine("📋 Current Extension Provider: ${currentProvider?.getExtensionId() ?: "None"}")
            sb.appendLine("📋 Current Extension Name: ${currentProvider?.getDisplayName() ?: "None"}")
        } catch (e: Exception) {
            sb.appendLine("❌ Extension Manager Error: ${e.message}")
        }
        
        // Check Plugin Context
        try {
            val pluginContext = project.getService(PluginContext::class.java)
            if (pluginContext != null) {
                sb.appendLine("✅ PluginContext: Available")
                
                val rpcProtocol = pluginContext.getRPCProtocol()
                if (rpcProtocol != null) {
                    sb.appendLine("✅ RPC Protocol: Available")
                    
                    val commandsProxy = rpcProtocol.getProxy(ServiceProxyRegistry.ExtHostContext.ExtHostCommands)
                    if (commandsProxy != null) {
                        sb.appendLine("✅ ExtHostCommands Proxy: Available")
                    } else {
                        sb.appendLine("❌ ExtHostCommands Proxy: Not Available")
                    }
                } else {
                    sb.appendLine("❌ RPC Protocol: Not Available")
                }
            } else {
                sb.appendLine("❌ PluginContext: Not Available")
            }
        } catch (e: Exception) {
            sb.appendLine("❌ Plugin Context Error: ${e.message}")
        }
        
        // Check available extensions
        try {
            val extensionManager = ExtensionManager.getInstance(project)
            val availableProviders = extensionManager.getAvailableProviders()
            sb.appendLine("\n📋 Available Extensions:")
            availableProviders.forEach { provider ->
                sb.appendLine("  - ${provider.getExtensionId()}: ${provider.getDisplayName()}")
            }
        } catch (e: Exception) {
            sb.appendLine("❌ Error getting available extensions: ${e.message}")
        }
        
        // Check WebView status
        try {
            val webViewManager = project.getService(WebViewManager::class.java)
            if (webViewManager != null) {
                sb.appendLine("\n🌐 WebView Status:")
                val latestWebView = webViewManager.getLatestWebView()
                if (latestWebView != null) {
                    sb.appendLine("✅ Latest WebView: Available")
                } else {
                    sb.appendLine("❌ Latest WebView: Not Available")
                }
            } else {
                sb.appendLine("\n❌ WebView Manager: Not Available")
            }
        } catch (e: Exception) {
            sb.appendLine("\n❌ WebView Status Error: ${e.message}")
        }
        
        return sb.toString()
    }
    
    private fun showStatusDialog(status: String) {
        Messages.showInfoMessage(status, "Extension Status")
    }
}
