// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.ui

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.ui.jcef.JBCefApp
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.ide.BrowserUtil
import com.sina.weibo.agent.actions.OpenDevToolsAction
import com.sina.weibo.agent.plugin.WecoderPlugin
import com.sina.weibo.agent.plugin.WecoderPluginService
import com.sina.weibo.agent.plugin.DEBUG_MODE
import com.sina.weibo.agent.webview.DragDropHandler
import com.sina.weibo.agent.webview.WebViewCreationCallback
import com.sina.weibo.agent.webview.WebViewInstance
import com.sina.weibo.agent.webview.WebViewManager
import com.sina.weibo.agent.util.PluginConstants
import com.sina.weibo.agent.extensions.core.ExtensionConfigurationManager
import com.sina.weibo.agent.extensions.core.ExtensionManager
import com.sina.weibo.agent.plugin.SystemObjectProvider
import java.awt.BorderLayout
import java.awt.datatransfer.StringSelection
import java.awt.Toolkit
import java.awt.Dimension
import java.awt.Font
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel

class RunVSAgentToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        // Initialize plugin service
        val pluginService = WecoderPlugin.getInstance(project)
//        pluginService.initialize(project)

        // toolbar
        val titleActions = mutableListOf<AnAction>()
        val action = ActionManager.getInstance().getAction("WecoderToolbarGroup")
        if (action != null) {
            titleActions.add(action)
        }
        // Add developer tools button only in debug mode
        if ( WecoderPluginService.getDebugMode() != DEBUG_MODE.NONE) {
            titleActions.add(OpenDevToolsAction { project.getService(WebViewManager::class.java).getLatestWebView() })
        }
        
        toolWindow.setTitleActions(titleActions)

        // webview panel
        val toolWindowContent = RunVSAgentToolWindowContent(project, toolWindow)
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(
            toolWindowContent.content,
            "",
            false
        )
        toolWindow.contentManager.addContent(content)
    }

    private class RunVSAgentToolWindowContent(
        private val project: Project,
        private val toolWindow: ToolWindow
    ) : WebViewCreationCallback {
        private val logger = Logger.getInstance(RunVSAgentToolWindowContent::class.java)
        
        // Get WebViewManager instance
        private val webViewManager = project.getService(WebViewManager::class.java)
        
        // Get ExtensionConfigurationManager instance
        private val configManager = ExtensionConfigurationManager.getInstance(project)
        
        // Content panel
        private val contentPanel = JPanel(BorderLayout())
        
        // Placeholder label
        private val placeholderLabel = JLabel(createSystemInfoText())

        // System info text for copying
        private val systemInfoText = createSystemInfoPlainText()
        
        // Plugin selection panel (shown when configuration is invalid)
        private val pluginSelectionPanel = createPluginSelectionPanel()
        
        // Configuration status panel
        private val configStatusPanel = createConfigStatusPanel()
        
        // State lock to prevent UI changes during plugin startup
        @Volatile
        private var isPluginStarting = false
        
        // Plugin running state
        @Volatile
        private var isPluginRunning = false
        
        /**
         * Check if plugin is actually running
         */
        private fun isPluginActuallyRunning(): Boolean {
            return try {
                val extensionManager = ExtensionManager.getInstance(project)
                extensionManager.isProperlyInitialized()
            } catch (e: Exception) {
                false
            }
        }
        
        /**
         * Create system information text in HTML format
         */
        private fun createSystemInfoText(): String {
            val appInfo = ApplicationInfo.getInstance()
            val plugin = PluginManagerCore.getPlugin(PluginId.getId(PluginConstants.PLUGIN_ID))
            val pluginVersion = plugin?.version ?: "unknown"
            val osName = System.getProperty("os.name")
            val osVersion = System.getProperty("os.version")
            val osArch = System.getProperty("os.arch")
            val jcefSupported = JBCefApp.isSupported()
            
            // Check for Linux ARM system
            val isLinuxArm = osName.lowercase().contains("linux") && (osArch.lowercase().contains("aarch64") || osArch.lowercase().contains("arm"))
            
            return buildString {
                append("<html><body style='width: 400px; font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, \"Helvetica Neue\", Arial, sans-serif; margin: 0; padding: 20px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; border-radius: 12px;'>")
                
                // Header section
                append("<div style='text-align: center; margin-bottom: 30px;'>")
                append("<div style='font-size: 24px; font-weight: 600; margin-bottom: 8px;'>🚀 RunVSAgent</div>")
                append("<div style='font-size: 14px; opacity: 0.9;'>正在初始化中...</div>")
                append("</div>")
                
                // System info card
                append("<div style='background: rgba(255, 255, 255, 0.15); backdrop-filter: blur(10px); border-radius: 8px; padding: 20px; margin-bottom: 20px; border: 1px solid rgba(255, 255, 255, 0.2);'>")
                append("<div style='font-size: 16px; font-weight: 600; margin-bottom: 15px; text-align: center;'>📊 系统信息</div>")
                append("<div style='display: grid; gap: 12px;'>")
                
                // Info rows with modern styling
                append("<div style='display: flex; justify-content: space-between; align-items: center; padding: 8px 0; border-bottom: 1px solid rgba(255, 255, 255, 0.1);'>")
                append("<span style='font-weight: 500; opacity: 0.9;'>💻 CPU 架构</span>")
                append("<span style='font-weight: 600;'>$osArch</span>")
                append("</div>")
                
                append("<div style='display: flex; justify-content: space-between; align-items: center; padding: 8px 0; border-bottom: 1px solid rgba(255, 255, 255, 0.1);'>")
                append("<span style='font-weight: 500; opacity: 0.9;'>🖥️ 操作系统</span>")
                append("<span style='font-weight: 600;'>$osName $osVersion</span>")
                append("</div>")
                
                append("<div style='display: flex; justify-content: space-between; align-items: center; padding: 8px 0; border-bottom: 1px solid rgba(255, 255, 255, 0.1);'>")
                append("<span style='font-weight: 500; opacity: 0.9;'>🔧 IDE 版本</span>")
                append("<span style='font-weight: 600; font-size: 12px;'>${appInfo.fullApplicationName}</span>")
                append("</div>")
                
                append("<div style='display: flex; justify-content: space-between; align-items: center; padding: 8px 0; border-bottom: 1px solid rgba(255, 255, 255, 0.1);'>")
                append("<span style='font-weight: 500; opacity: 0.9;'>📦 插件版本</span>")
                append("<span style='font-weight: 600;'>$pluginVersion</span>")
                append("</div>")
                
                append("<div style='display: flex; justify-content: space-between; align-items: center; padding: 8px 0;'>")
                append("<span style='font-weight: 500; opacity: 0.9;'>🌐 JCEF 支持</span>")
                append("<span style='font-weight: 600; color: ${if (jcefSupported) "#4ade80" else "#f87171"};'>${if (jcefSupported) "✓ 是" else "✗ 否"}</span>")
                append("</div>")
                
                append("</div>")
                append("</div>")
                
                // Warning messages with modern styling
                if (isLinuxArm) {
                    append("<div style='background: rgba(255, 193, 7, 0.2); border: 1px solid rgba(255, 193, 7, 0.4); border-radius: 8px; padding: 16px; margin-bottom: 16px; backdrop-filter: blur(10px);'>")
                    append("<div style='display: flex; align-items: center; margin-bottom: 8px;'>")
                    append("<span style='font-size: 18px; margin-right: 8px;'>⚠️</span>")
                    append("<span style='font-weight: 600; color: #fbbf24;'>系统不受支持</span>")
                    append("</div>")
                    append("<div style='font-size: 13px; opacity: 0.9; line-height: 1.4;'>Linux ARM 系统目前不受此插件支持。</div>")
                    append("</div>")
                }
                
                if (!jcefSupported) {
                    append("<div style='background: rgba(239, 68, 68, 0.2); border: 1px solid rgba(239, 68, 68, 0.4); border-radius: 8px; padding: 16px; margin-bottom: 16px; backdrop-filter: blur(10px);'>")
                    append("<div style='display: flex; align-items: center; margin-bottom: 8px;'>")
                    append("<span style='font-size: 18px; margin-right: 8px;'>❌</span>")
                    append("<span style='font-weight: 600; color: #f87171;'>JCEF 不受支持</span>")
                    append("</div>")
                    append("<div style='font-size: 13px; opacity: 0.9; line-height: 1.4;'>您的 IDE 运行时不支持 JCEF。请使用支持 JCEF 的运行时。</div>")
                    append("</div>")
                }
                
                // Help text
                append("<div style='text-align: center; margin-top: 20px; padding: 16px; background: rgba(255, 255, 255, 0.1); border-radius: 8px; backdrop-filter: blur(10px);'>")
                append("<div style='font-size: 13px; opacity: 0.9; line-height: 1.5;'>")
                append("如果此界面持续显示较长时间，您可以参考已知问题文档来检查是否存在已知问题。")
                append("</div>")
                append("</div>")
                
                append("</body></html>")
            }
        }
        
        /**
         * Create system information text in plain text format for copying
         */
        private fun createSystemInfoPlainText(): String {
            val appInfo = ApplicationInfo.getInstance()
            val plugin = PluginManagerCore.getPlugin(PluginId.getId(PluginConstants.PLUGIN_ID))
            val pluginVersion = plugin?.version ?: "unknown"
            val osName = System.getProperty("os.name")
            val osVersion = System.getProperty("os.version")
            val osArch = System.getProperty("os.arch")
            val jcefSupported = JBCefApp.isSupported()
            
            // Check for Linux ARM system
            val isLinuxArm = osName.lowercase().contains("linux") && (osArch.lowercase().contains("aarch64") || osArch.lowercase().contains("arm"))
            
            return buildString {
                append("RunVSAgent 系统信息\n")
                append("==================\n\n")
                append("🚀 插件状态: 正在初始化中...\n\n")
                append("📊 系统信息:\n")
                append("  💻 CPU 架构: $osArch\n")
                append("  🖥️ 操作系统: $osName $osVersion\n")
                append("  🔧 IDE 版本: ${appInfo.fullApplicationName} (build ${appInfo.build})\n")
                append("  📦 插件版本: $pluginVersion\n")
                append("  🌐 JCEF 支持: ${if (jcefSupported) "✓ 是" else "✗ 否"}\n\n")
                
                // Add warning messages
                if (isLinuxArm) {
                    append("⚠️ 警告: 系统不受支持\n")
                    append("   Linux ARM 系统目前不受此插件支持。\n\n")
                }
                
                if (!jcefSupported) {
                    append("❌ 警告: JCEF 不受支持\n")
                    append("   您的 IDE 运行时不支持 JCEF。请使用支持 JCEF 的运行时。\n")
                    append("   请参考已知问题文档获取更多信息。\n\n")
                }
                
                append("💡 提示: 如果此界面持续显示较长时间，您可以参考已知问题文档来检查是否存在已知问题。\n")
            }
        }
        
        /**
         * Copy system information to clipboard
         */
        private fun copySystemInfo() {
            val stringSelection = StringSelection(systemInfoText)
            val clipboard = Toolkit.getDefaultToolkit().getSystemClipboard()
            clipboard.setContents(stringSelection, null)
        }
        
        // Known Issues button
        private val knownIssuesButton = JButton("📚 已知问题").apply {
            preferredSize = Dimension(160, 36)
            font = font.deriveFont(14f)
            isOpaque = true
            background = java.awt.Color(99, 102, 241, 255) // Indigo color
            foreground = java.awt.Color.WHITE
            border = javax.swing.BorderFactory.createEmptyBorder(8, 16, 8, 16)
            isFocusPainted = false
            addActionListener {
                BrowserUtil.browse("https://github.com/wecode-ai/RunVSAgent/blob/main/docs/KNOWN_ISSUES.md")
            }
        }
        
        // Copy button
        private val copyButton = JButton("📋 复制系统信息").apply {
            preferredSize = Dimension(160, 36)
            font = font.deriveFont(14f)
            isOpaque = true
            background = java.awt.Color(34, 197, 94, 255) // Green color
            foreground = java.awt.Color.WHITE
            border = javax.swing.BorderFactory.createEmptyBorder(8, 16, 8, 16)
            isFocusPainted = false
            addActionListener { copySystemInfo() }
        }
        
        // Button panel to hold both buttons side by side with modern spacing
        private val buttonPanel = JPanel().apply {
            layout = BorderLayout()
            border = javax.swing.BorderFactory.createEmptyBorder(20, 0, 0, 0)
            add(knownIssuesButton, BorderLayout.WEST)
            add(copyButton, BorderLayout.EAST)
        }
        
        private var dragDropHandler: DragDropHandler? = null
        
        // Main panel
        val content: JPanel = JPanel(BorderLayout()).apply {
            // Set content panel with both label and button
            contentPanel.layout = BorderLayout()
            
            // Check configuration status and show appropriate content
            if (configManager.isConfigurationLoaded() && configManager.isConfigurationValid()) {
                // Configuration is valid, show system info
                contentPanel.add(placeholderLabel, BorderLayout.CENTER)
                contentPanel.add(buttonPanel, BorderLayout.SOUTH)
            } else {
                // Configuration is invalid, show plugin selection
                contentPanel.add(pluginSelectionPanel, BorderLayout.CENTER)
                contentPanel.add(configStatusPanel, BorderLayout.SOUTH)
            }
            
            add(contentPanel, BorderLayout.CENTER)
        }
        
        init {
            // Initialize UI content based on current configuration status
            updateUIContent()
            
            // Start configuration monitoring
            startConfigurationMonitoring()
            
            // Try to get existing WebView
            webViewManager.getLatestWebView()?.let { webView ->
                // Add WebView component immediately when created
                ApplicationManager.getApplication().invokeLater {
                    addWebViewComponent(webView)
                }
                // Set page load callback to hide system info only after page is loaded
                webView.setPageLoadCallback {
                    ApplicationManager.getApplication().invokeLater {
                        hideSystemInfo()
                    }
                }
                // If page is already loaded, hide system info immediately
                if (webView.isPageLoaded()) {
                    ApplicationManager.getApplication().invokeLater {
                        hideSystemInfo()
                    }
                }
            }?:webViewManager.addCreationCallback(this, toolWindow.disposable)
        }
        
        /**
         * Start configuration monitoring to detect changes
         */
        private fun startConfigurationMonitoring() {
            // Start background monitoring thread
            Thread {
                try {
                    while (!project.isDisposed) {
                        Thread.sleep(2000) // Check every 2 seconds
                        
                        if (!project.isDisposed) {
                            // Don't update UI if plugin is starting or running
                            if (isPluginStarting || isPluginRunning) {
                                logger.debug("Plugin is starting or running, skipping UI update")
                                continue
                            }
                            
                            // Only update UI if we're not in the middle of plugin startup
                            // Check if plugin is actually running before updating UI
                            val isPluginRunning = isPluginActuallyRunning()
                            
                            // Only update UI if plugin is not running or if there's a significant change
                            if (!isPluginRunning) {
                                ApplicationManager.getApplication().invokeLater {
                                    updateUIContent()
                                }
                            } else {
                                // Plugin is running, only update status labels, don't change main UI
                                ApplicationManager.getApplication().invokeLater {
                                    updateConfigStatus(configStatusPanel.getComponent(0) as JLabel)
                                }
                            }
                        }
                    }
                } catch (e: InterruptedException) {
                    logger.info("Configuration monitoring interrupted")
                } catch (e: Exception) {
                    logger.error("Error in configuration monitoring", e)
                }
            }.apply {
                isDaemon = true
                name = "RunVSAgent-ConfigMonitor-UI"
                start()
            }
        }
        
        /**
         * WebView creation callback implementation
         */
        override fun onWebViewCreated(instance: WebViewInstance) {
            // Add WebView component immediately when created
            ApplicationManager.getApplication().invokeLater {
                addWebViewComponent(instance)
            }
            // Set page load callback to hide system info only after page is loaded
            instance.setPageLoadCallback {
                // Ensure UI update in EDT thread
                ApplicationManager.getApplication().invokeLater {
                    hideSystemInfo()
                }
            }
        }
        
        /**
         * Add WebView component to UI
         */
        private fun addWebViewComponent(webView: WebViewInstance) {
            logger.info("Adding WebView component to UI: ${webView.viewType}/${webView.viewId}")
            
            // Check if WebView component is already added
            val components = contentPanel.components
            for (component in components) {
                if (component === webView.browser.component) {
                    logger.info("WebView component already exists in UI")
                    return
                }
            }
            
            // Add WebView component without removing existing components
            contentPanel.add(webView.browser.component, BorderLayout.CENTER)
            
            setupDragAndDropSupport(webView)
            
            // Relayout
            contentPanel.revalidate()
            contentPanel.repaint()
            
            logger.info("WebView component added to tool window")
        }
        
        /**
         * Hide system info placeholder
         */
        private fun hideSystemInfo() {
            logger.info("Hiding system info placeholder")
            
            // Remove all components from content panel except WebView component
            val components = contentPanel.components
            for (component in components) {
                if (component !== webViewManager.getLatestWebView()?.browser?.component) {
                    contentPanel.remove(component)
                }
            }

            // Relayout
            contentPanel.revalidate()
            contentPanel.repaint()
            
            logger.info("System info placeholder hidden")
        }
        
        /**
         * Setup drag and drop support
         */
        private fun setupDragAndDropSupport(webView: WebViewInstance) {
            try {
                logger.info("Setting up drag and drop support for WebView")
                
                dragDropHandler = DragDropHandler(webView, contentPanel)
                
                dragDropHandler?.setupDragAndDrop()
                
                logger.info("Drag and drop support enabled")
            } catch (e: Exception) {
                logger.error("Failed to setup drag and drop support", e)
            }
        }
        
        /**
         * Create plugin selection panel
         */
        private fun createPluginSelectionPanel(): JPanel {
            val panel = JPanel()
            panel.layout = BorderLayout()
            panel.border = javax.swing.BorderFactory.createEmptyBorder(20, 20, 20, 20)
            
            // Title
            val titleLabel = JLabel("🔧 选择默认插件").apply {
                font = font.deriveFont(18f)
                horizontalAlignment = javax.swing.SwingConstants.CENTER
                border = javax.swing.BorderFactory.createEmptyBorder(0, 0, 20, 0)
            }
            
            // Description
            val descLabel = JLabel("检测到配置无效，请选择一个默认插件继续使用：").apply {
                font = font.deriveFont(14f)
                horizontalAlignment = javax.swing.SwingConstants.CENTER
                border = javax.swing.BorderFactory.createEmptyBorder(0, 0, 20, 0)
            }
            
            // Plugin options
            val pluginOptionsPanel = JPanel()
            pluginOptionsPanel.layout = javax.swing.BoxLayout(pluginOptionsPanel, javax.swing.BoxLayout.Y_AXIS)
            
            val plugins = listOf(
                "roo-code" to "Roo Code - AI驱动的代码助手",
                "cline" to "Cline - 高级AI编程助手",
                "custom" to "自定义插件"
            )
            
            val buttonGroup = javax.swing.ButtonGroup()
            val radioButtons = mutableListOf<javax.swing.JRadioButton>()
            
            plugins.forEach { (id, description) ->
                val radioButton = javax.swing.JRadioButton("$id: $description").apply {
                    font = font.deriveFont(14f)
                    border = javax.swing.BorderFactory.createEmptyBorder(8, 0, 8, 0)
                    actionCommand = id
                }
                radioButtons.add(radioButton)
                buttonGroup.add(radioButton)
                pluginOptionsPanel.add(radioButton)
                
                // Set roo-code as default selected
                if (id == "roo-code") {
                    radioButton.isSelected = true
                }
            }
            
            // Action buttons
            val buttonPanel = JPanel()
            buttonPanel.layout = BorderLayout()
            buttonPanel.border = javax.swing.BorderFactory.createEmptyBorder(20, 0, 0, 0)
            
            val applyButton = JButton("✅ 应用并继续").apply {
                preferredSize = Dimension(160, 36)
                font = font.deriveFont(14f)
                isOpaque = true
                background = java.awt.Color(34, 197, 94, 255) // Green color
                foreground = java.awt.Color.WHITE
                border = javax.swing.BorderFactory.createEmptyBorder(8, 16, 8, 16)
                isFocusPainted = false
                addActionListener {
                    val selectedPlugin = radioButtons.find { it.isSelected }?.actionCommand
                    if (selectedPlugin != null) {
                        applyPluginSelection(selectedPlugin)
                    }
                }
            }
            
            val manualButton = JButton("📝 手动配置").apply {
                preferredSize = Dimension(160, 36)
                font = font.deriveFont(14f)
                isOpaque = true
                background = java.awt.Color(59, 130, 246, 255) // Blue color
                foreground = java.awt.Color.WHITE
                border = javax.swing.BorderFactory.createEmptyBorder(8, 16, 8, 16)
                isFocusPainted = false
                addActionListener {
                    showManualConfigInstructions()
                }
            }
            
            val debugButton = JButton("🐛 调试信息").apply {
                preferredSize = Dimension(160, 36)
                font = font.deriveFont(14f)
                isOpaque = true
                background = java.awt.Color(168, 85, 247, 255) // Purple color
                foreground = java.awt.Color.WHITE
                border = javax.swing.BorderFactory.createEmptyBorder(8, 16, 8, 16)
                isFocusPainted = false
                addActionListener {
                    showDebugInfo()
                }
            }
            
            buttonPanel.add(applyButton, BorderLayout.WEST)
            buttonPanel.add(manualButton, BorderLayout.CENTER)
            buttonPanel.add(debugButton, BorderLayout.EAST)
            
            // Add all components
            panel.add(titleLabel, BorderLayout.NORTH)
            panel.add(descLabel, BorderLayout.CENTER)
            panel.add(pluginOptionsPanel, BorderLayout.CENTER)
            panel.add(buttonPanel, BorderLayout.SOUTH)
            
            return panel
        }
        
        /**
         * Create configuration status panel
         */
        private fun createConfigStatusPanel(): JPanel {
            val panel = JPanel()
            panel.layout = BorderLayout()
            panel.border = javax.swing.BorderFactory.createEmptyBorder(20, 20, 20, 20)
            
            // Status label
            val statusLabel = JLabel().apply {
                font = font.deriveFont(14f)
                horizontalAlignment = javax.swing.SwingConstants.CENTER
            }
            
            // Update status
            updateConfigStatus(statusLabel)
            
            panel.add(statusLabel, BorderLayout.CENTER)
            return panel
        }
        
        /**
         * Update configuration status
         */
        private fun updateConfigStatus(statusLabel: JLabel) {
            if (configManager.isConfigurationLoaded()) {
                if (configManager.isConfigurationValid()) {
                    val extensionId = configManager.getCurrentExtensionId()
                    // Check if plugin is actually running
                    val isPluginRunning = isPluginActuallyRunning()
                    
                    if (isPluginRunning) {
                        statusLabel.text = "✅ 插件运行中 - 当前插件: $extensionId"
                        statusLabel.foreground = java.awt.Color(34, 197, 94) // Green
                    } else {
                        statusLabel.text = "⚠️ 配置有效但插件未运行 - 当前插件: $extensionId"
                        statusLabel.foreground = java.awt.Color(245, 158, 11) // Yellow
                    }
                } else {
                    statusLabel.text = "❌ 配置无效 - ${configManager.getConfigurationError()}"
                    statusLabel.foreground = java.awt.Color(239, 68, 68) // Red
                }
            } else {
                statusLabel.text = "⏳ 配置加载中..."
                statusLabel.foreground = java.awt.Color(59, 130, 246) // Blue
            }
        }
        
        /**
         * Apply plugin selection and create configuration
         */
        private fun applyPluginSelection(pluginId: String) {
            try {
                logger.info("Applying plugin selection: $pluginId")
                
                // Create configuration with selected plugin
                configManager.setCurrentExtensionId(pluginId)
                
                // Verify configuration was saved successfully
                if (configManager.isConfigurationValid()) {
                    // Show success message
                    val message = "✅ 已选择插件: $pluginId\n正在启动插件..."
                    javax.swing.JOptionPane.showMessageDialog(
                        contentPanel,
                        message,
                        "配置已更新",
                        javax.swing.JOptionPane.INFORMATION_MESSAGE
                    )
                    
                    // Start the plugin directly instead of just saving configuration
                    startPluginAfterSelection(pluginId)
                    
                    logger.info("Plugin selection applied successfully: $pluginId")
                } else {
                    // Configuration is still invalid after setting
                    val errorMsg = configManager.getConfigurationError() ?: "Unknown error"
                    val message = "❌ 配置更新失败\n错误信息: $errorMsg\n\n请检查配置文件或尝试手动配置。"
                    javax.swing.JOptionPane.showMessageDialog(
                        contentPanel,
                        message,
                        "配置更新失败",
                        javax.swing.JOptionPane.ERROR_MESSAGE
                    )
                    
                    logger.error("Configuration is still invalid after setting extension ID: $pluginId, error: $errorMsg")
                }
            } catch (e: Exception) {
                logger.error("Failed to apply plugin selection", e)
                val message = "❌ 配置更新失败\n错误信息: ${e.message}\n\n请检查文件权限或尝试手动配置。"
                javax.swing.JOptionPane.showMessageDialog(
                    contentPanel,
                    message,
                    "错误",
                    javax.swing.JOptionPane.ERROR_MESSAGE
                )
            }
        }
        
        /**
         * Start plugin after plugin selection
         */
        private fun startPluginAfterSelection(pluginId: String) {
            try {
                logger.info("Starting plugin after selection: $pluginId")
                
                // Set plugin starting state
                isPluginStarting = true
                
                // Update status to show plugin is starting
                updateConfigStatus(configStatusPanel.getComponent(0) as JLabel)
                
                // Get extension manager and set the selected provider
                val extensionManager = ExtensionManager.getInstance(project)
                extensionManager.initialize(pluginId)
                
                // Initialize the current provider
                extensionManager.initializeCurrentProvider()
                
                // Start plugin service
                val pluginService = WecoderPlugin.getInstance(project)
                pluginService.initialize(project)
                
                // Initialize WebViewManager
                val webViewManager = project.getService(WebViewManager::class.java)
                if (webViewManager != null) {
                    // Register to project Disposer
                    com.intellij.openapi.util.Disposer.register(project, webViewManager)
                    
                    // Start configuration monitoring
                    startConfigurationMonitoring()
                    
                    // Register project-level resource disposal
                    com.intellij.openapi.util.Disposer.register(project, com.intellij.openapi.Disposable {
                        logger.info("Disposing RunVSAgent plugin for project: ${project.name}")
                        pluginService.dispose()
                        extensionManager.dispose()
                        SystemObjectProvider.dispose()
                        // Reset state when disposing
                        isPluginRunning = false
                        isPluginStarting = false
                    })
                    
                    logger.info("Plugin started successfully after selection: $pluginId")
                    
                    // Set plugin running state
                    isPluginRunning = true
                    isPluginStarting = false
                    
                    // Show success message
                    val successMessage = "🎉 插件启动成功！\n插件: $pluginId\n现在可以使用插件功能了。"
                    javax.swing.JOptionPane.showMessageDialog(
                        contentPanel,
                        successMessage,
                        "插件启动成功",
                        javax.swing.JOptionPane.INFORMATION_MESSAGE
                    )
                    
                    // Update UI to show plugin is running
                    updateUIContent()
                } else {
                    logger.error("WebViewManager not available")
                    throw IllegalStateException("WebViewManager not available")
                }
                
            } catch (e: Exception) {
                logger.error("Failed to start plugin after selection", e)
                // Reset state on failure
                isPluginStarting = false
                isPluginRunning = false
                
                val message = "❌ 插件启动失败\n错误信息: ${e.message}\n\n请检查插件配置或尝试重启IDE。"
                javax.swing.JOptionPane.showMessageDialog(
                    contentPanel,
                    message,
                    "插件启动失败",
                    javax.swing.JOptionPane.ERROR_MESSAGE
                )
            }
        }
        
        /**
         * Update UI content based on configuration status
         */
        private fun updateUIContent() {
            // Don't update UI if plugin is starting or running
            if (isPluginStarting || isPluginRunning) {
                logger.info("Plugin is starting or running, skipping UI update")
                return
            }
            
            // Check if plugin is actually running
            val isPluginRunning = isPluginActuallyRunning()
            
            // If plugin is running, don't change the main UI content
            if (isPluginRunning) {
                logger.info("Plugin is running, keeping current UI content")
                return
            }
            
            contentPanel.removeAll()
            
            if (configManager.isConfigurationLoaded() && configManager.isConfigurationValid()) {
                // Configuration is valid, show system info
                contentPanel.add(placeholderLabel, BorderLayout.CENTER)
                contentPanel.add(buttonPanel, BorderLayout.SOUTH)
                logger.info("Showing system info panel - configuration is valid")
            } else {
                // Configuration is invalid, show plugin selection
                contentPanel.add(pluginSelectionPanel, BorderLayout.CENTER)
                contentPanel.add(configStatusPanel, BorderLayout.SOUTH)
                logger.info("Showing plugin selection panel - configuration is invalid")
            }
            
            contentPanel.revalidate()
            contentPanel.repaint()
        }
        
        /**
         * Show manual configuration instructions
         */
        private fun showManualConfigInstructions() {
            val instructions = """
                📝 手动配置说明
                
                1. 在项目根目录创建文件: ${PluginConstants.ConfigFiles.MAIN_CONFIG_FILE}
                2. 添加以下内容:
                   ${PluginConstants.ConfigFiles.EXTENSION_TYPE_KEY}=roo-code
                   
                3. 支持的插件类型:
                   - roo-code: Roo Code AI助手
                   - cline: Cline AI助手
                   - custom: 自定义插件
                   
                4. 保存文件后重启IDE
                
                配置文件路径: ${configManager.getConfigurationFilePath()}
            """.trimIndent()
            
            javax.swing.JOptionPane.showMessageDialog(
                contentPanel,
                instructions,
                "手动配置说明",
                javax.swing.JOptionPane.INFORMATION_MESSAGE
            )
        }

        /**
         * Show debug information
         */
        private fun showDebugInfo() {
            val debugText = """
                RunVSAgent 调试信息
                ==================
                
                🚀 插件状态: ${if (configManager.isConfigurationLoaded() && configManager.isConfigurationValid()) "已加载且有效" else "未加载或无效"}
                
                📝 当前配置: ${configManager.getCurrentExtensionId() ?: "未设置"}
                
                ⚙️ 配置文件路径: ${configManager.getConfigurationFilePath()}
                
                🔄 配置加载时间: ${configManager.getConfigurationLoadTime()?.let { it.toString() } ?: "未知"}
                
                💡 提示: 如果配置无效，请检查配置文件内容或尝试手动配置。
            """.trimIndent()
            
            javax.swing.JOptionPane.showMessageDialog(
                contentPanel,
                debugText,
                "调试信息",
                javax.swing.JOptionPane.INFORMATION_MESSAGE
            )
        }
    }
}