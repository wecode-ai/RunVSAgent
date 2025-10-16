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
import com.sina.weibo.agent.extensions.ui.VsixUploadDialog
import java.awt.BorderLayout
import java.awt.datatransfer.StringSelection
import java.awt.Toolkit
import java.awt.Dimension
import java.awt.Font
import java.awt.Component
import java.awt.Cursor
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.BorderFactory
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.sina.weibo.agent.util.ConfigFileUtils

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
        
        // Get ExtensionManager instance
        private val extensionManager = ExtensionManager.getInstance(project)

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

            // Detect current IDEA theme
            val isDarkTheme = detectCurrentTheme()
            
            // Generate theme-adaptive CSS styles
            val themeStyles = generateThemeStyles(isDarkTheme)

            return buildString {
                append("<html><head><style>$themeStyles</style></head>")
                append("<body class='${if (isDarkTheme) "dark-theme" else "light-theme"}'>")

                // Header section
                append("<div class='header'>")
                append("<div class='title'>🚀 RunVSAgent</div>")
                append("<div class='subtitle'>Initializing...</div>")
                append("</div>")

                // System info card
                append("<div class='info-card'>")
                append("<div class='card-title'>📊 System Information</div>")
                append("<div class='info-grid'>")

                // Info rows with modern styling
                append("<div class='info-row'>")
                append("<span class='info-label'>💻 CPU Architecture</span>")
                append("<span class='info-value'>$osArch</span>")
                append("</div>")

                append("<div class='info-row'>")
                append("<span class='info-label'>🖥️ Operating System</span>")
                append("<span class='info-value'>$osName $osVersion</span>")
                append("</div>")

                append("<div class='info-row'>")
                append("<span class='info-label'>🔧 IDE Version</span>")
                append("<span class='info-value version-text'>${appInfo.fullApplicationName}</span>")
                append("</div>")

                append("<div class='info-row'>")
                append("<span class='info-label'>📦 Plugin Version</span>")
                append("<span class='info-value'>$pluginVersion</span>")
                append("</div>")

                append("<div class='info-row'>")
                append("<span class='info-label'>🌐 JCEF Support</span>")
                append("<span class='info-value ${if (jcefSupported) "success" else "error"}'>${if (jcefSupported) "✓ Yes" else "✗ No"}</span>")
                append("</div>")

                append("</div>")
                append("</div>")

                // Warning messages with modern styling
                if (isLinuxArm) {
                    append("<div class='warning-card warning'>")
                    append("<div class='warning-header'>")
                    append("<span class='warning-icon'>⚠️</span>")
                    append("<span class='warning-title'>System Not Supported</span>")
                    append("</div>")
                    append("<div class='warning-text'>Linux ARM systems are currently not supported by this plugin.</div>")
                    append("</div>")
                }

                if (!jcefSupported) {
                    append("<div class='warning-card error'>")
                    append("<div class='warning-header'>")
                    append("<span class='warning-icon'>❌</span>")
                    append("<span class='warning-title'>JCEF Not Supported</span>")
                    append("</div>")
                    append("<div class='warning-text'>Your IDE runtime does not support JCEF. Please use a runtime that supports JCEF.</div>")
                    append("</div>")
                }

                // Help text
                append("<div class='help-card'>")
                append("<div class='help-text'>")
                append("If this interface continues to display for a long time, you can refer to the known issues documentation to check if there are any known problems.")
                append("</div>")
                append("</div>")

                append("</body></html>")
            }
        }

        /**
         * Detect current IDEA theme
         */
        private fun detectCurrentTheme(): Boolean {
            return try {
                val background = javax.swing.UIManager.getColor("Panel.background")
                if (background != null) {
                    val brightness = (0.299 * background.red + 0.587 * background.green + 0.114 * background.blue) / 255.0
                    brightness < 0.5
                } else {
                    // Default to dark theme if cannot detect
                    true
                }
            } catch (e: Exception) {
                // Default to dark theme on error
                true
            }
        }

        /**
         * Generate theme-adaptive CSS styles
         */
        private fun generateThemeStyles(isDarkTheme: Boolean): String {
            return if (isDarkTheme) {
                """
                body.dark-theme {
                    width: 400px;
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
                    margin: 0;
                    padding: 20px;
                    background: linear-gradient(135deg, #1e293b 0%, #334155 100%);
                    color: #e2e8f0;
                    border-radius: 12px;
                }
                
                .header {
                    text-align: center;
                    margin-bottom: 30px;
                }
                
                .title {
                    font-size: 24px;
                    font-weight: 600;
                    margin-bottom: 8px;
                    color: #f8fafc;
                }
                
                .subtitle {
                    font-size: 14px;
                    opacity: 0.9;
                    color: #cbd5e1;
                }
                
                .info-card {
                    background: rgba(30, 41, 59, 0.8);
                    backdrop-filter: blur(10px);
                    border-radius: 8px;
                    padding: 20px;
                    margin-bottom: 20px;
                    border: 1px solid rgba(148, 163, 184, 0.2);
                }
                
                .card-title {
                    font-size: 16px;
                    font-weight: 600;
                    margin-bottom: 15px;
                    text-align: center;
                    color: #f1f5f9;
                }
                
                .info-grid {
                    display: grid;
                    gap: 12px;
                }
                
                .info-row {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    padding: 8px 0;
                    border-bottom: 1px solid rgba(148, 163, 184, 0.1);
                }
                
                .info-row:last-child {
                    border-bottom: none;
                }
                
                .info-label {
                    font-weight: 500;
                    opacity: 0.9;
                    color: #cbd5e1;
                }
                
                .info-value {
                    font-weight: 600;
                    color: #f8fafc;
                }
                
                .version-text {
                    font-size: 12px;
                }
                
                .success {
                    color: #10b981;
                }
                
                .error {
                    color: #ef4444;
                }
                
                .warning-card {
                    border-radius: 8px;
                    padding: 16px;
                    margin-bottom: 16px;
                    backdrop-filter: blur(10px);
                }
                
                .warning-card.warning {
                    background: rgba(245, 158, 11, 0.2);
                    border: 1px solid rgba(245, 158, 11, 0.4);
                }
                
                .warning-card.error {
                    background: rgba(239, 68, 68, 0.2);
                    border: 1px solid rgba(239, 68, 68, 0.4);
                }
                
                .warning-header {
                    display: flex;
                    align-items: center;
                    margin-bottom: 8px;
                }
                
                .warning-icon {
                    font-size: 18px;
                    margin-right: 8px;
                }
                
                .warning-title {
                    font-weight: 600;
                    color: #fbbf24;
                }
                
                .warning-card.error .warning-title {
                    color: #f87171;
                }
                
                .warning-text {
                    font-size: 13px;
                    opacity: 0.9;
                    line-height: 1.4;
                    color: #cbd5e1;
                }
                
                .help-card {
                    text-align: center;
                    margin-top: 20px;
                    padding: 16px;
                    background: rgba(30, 41, 59, 0.6);
                    border-radius: 8px;
                    backdrop-filter: blur(10px);
                    border: 1px solid rgba(148, 163, 184, 0.1);
                }
                
                .help-text {
                    font-size: 13px;
                    opacity: 0.9;
                    line-height: 1.5;
                    color: #cbd5e1;
                }
                """.trimIndent()
            } else {
                """
                body.light-theme {
                    width: 400px;
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
                    margin: 0;
                    padding: 20px;
                    background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
                    color: #334155;
                    border-radius: 12px;
                }
                
                .header {
                    text-align: center;
                    margin-bottom: 30px;
                }
                
                .title {
                    font-size: 24px;
                    font-weight: 600;
                    margin-bottom: 8px;
                    color: #1e293b;
                }
                
                .subtitle {
                    font-size: 14px;
                    opacity: 0.9;
                    color: #64748b;
                }
                
                .info-card {
                    background: rgba(255, 255, 255, 0.8);
                    backdrop-filter: blur(10px);
                    border-radius: 8px;
                    padding: 20px;
                    margin-bottom: 20px;
                    border: 1px solid rgba(148, 163, 184, 0.2);
                    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
                }
                
                .card-title {
                    font-size: 16px;
                    font-weight: 600;
                    margin-bottom: 15px;
                    text-align: center;
                    color: #1e293b;
                }
                
                .info-grid {
                    display: grid;
                    gap: 12px;
                }
                
                .info-row {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    padding: 8px 0;
                    border-bottom: 1px solid rgba(148, 163, 184, 0.2);
                }
                
                .info-row:last-child {
                    border-bottom: none;
                }
                
                .info-label {
                    font-weight: 500;
                    opacity: 0.9;
                    color: #64748b;
                }
                
                .info-value {
                    font-weight: 600;
                    color: #1e293b;
                }
                
                .version-text {
                    font-size: 12px;
                }
                
                .success {
                    color: #059669;
                }
                
                .error {
                    color: #dc2626;
                }
                
                .warning-card {
                    border-radius: 8px;
                    padding: 16px;
                    margin-bottom: 16px;
                    backdrop-filter: blur(10px);
                    box-shadow: 0 2px 4px -1px rgba(0, 0, 0, 0.1);
                }
                
                .warning-card.warning {
                    background: rgba(245, 158, 11, 0.1);
                    border: 1px solid rgba(245, 158, 11, 0.3);
                }
                
                .warning-card.error {
                    background: rgba(239, 68, 68, 0.1);
                    border: 1px solid rgba(239, 68, 68, 0.3);
                }
                
                .warning-header {
                    display: flex;
                    align-items: center;
                    margin-bottom: 8px;
                }
                
                .warning-icon {
                    font-size: 18px;
                    margin-right: 8px;
                }
                
                .warning-title {
                    font-weight: 600;
                    color: #d97706;
                }
                
                .warning-card.error .warning-title {
                    color: #dc2626;
                }
                
                .warning-text {
                    font-size: 13px;
                    opacity: 0.9;
                    line-height: 1.4;
                    color: #475569;
                }
                
                .help-card {
                    text-align: center;
                    margin-top: 20px;
                    padding: 16px;
                    background: rgba(255, 255, 255, 0.6);
                    border-radius: 8px;
                    backdrop-filter: blur(10px);
                    border: 1px solid rgba(148, 163, 184, 0.1);
                    box-shadow: 0 2px 4px -1px rgba(0, 0, 0, 0.1);
                }
                
                .help-text {
                    font-size: 13px;
                    opacity: 0.9;
                    line-height: 1.5;
                    color: #475569;
                }
                """.trimIndent()
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
                append("RunVSAgent System Information\n")
                append("=============================\n\n")
                append("🚀 Plugin Status: Initializing...\n\n")
                append("📊 System Information:\n")
                append("  💻 CPU Architecture: $osArch\n")
                append("  🖥️ Operating System: $osName $osVersion\n")
                append("  🔧 IDE Version: ${appInfo.fullApplicationName} (build ${appInfo.build})\n")
                append("  📦 Plugin Version: $pluginVersion\n")
                append("  🌐 JCEF Support: ${if (jcefSupported) "✓ Yes" else "✗ No"}\n\n")

                // Add warning messages
                if (isLinuxArm) {
                    append("⚠️ Warning: System Not Supported\n")
                    append("   Linux ARM systems are currently not supported by this plugin.\n\n")
                }

                if (!jcefSupported) {
                    append("❌ Warning: JCEF Not Supported\n")
                    append("   Your IDE runtime does not support JCEF. Please use a runtime that supports JCEF.\n")
                    append("   Please refer to the known issues documentation for more information.\n\n")
                }

                append("💡 Tip: If this interface continues to display for a long time, you can refer to the known issues documentation to check if there are any known problems.\n")
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
        private val knownIssuesButton = JButton("📚 Known Issues").apply {
            preferredSize = Dimension(160, 36)
            font = font.deriveFont(14f)
            isOpaque = false
            isFocusPainted = false
            border = javax.swing.BorderFactory.createEmptyBorder(8, 16, 8, 16)
            addActionListener {
                BrowserUtil.browse("https://github.com/wecode-ai/RunVSAgent/blob/main/docs/KNOWN_ISSUES.md")
            }
        }

        // Copy button
        private val copyButton = JButton("📋 Copy System Info").apply {
            preferredSize = Dimension(160, 36)
            font = font.deriveFont(14f)
            isOpaque = false
            isFocusPainted = false
            border = javax.swing.BorderFactory.createEmptyBorder(8, 16, 8, 16)
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

            // Add theme change listener
            addThemeChangeListener()

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
         * Add theme change listener to automatically update UI when theme changes
         */
        private fun addThemeChangeListener() {
            try {
                val messageBus = ApplicationManager.getApplication().messageBus
                val connection = messageBus.connect(toolWindow.disposable)
                connection.subscribe(com.intellij.ide.ui.LafManagerListener.TOPIC, com.intellij.ide.ui.LafManagerListener {
                    logger.info("Theme changed, updating UI styles")
                    // Update UI content with new theme
                    ApplicationManager.getApplication().invokeLater {
                        updateUIContent()
                        // Update status panel if it exists
                        if (configStatusPanel.componentCount > 0) {
                            updateConfigStatus(configStatusPanel.getComponent(0) as JLabel)
                        }
                    }
                })
                logger.info("Theme change listener added successfully")
            } catch (e: Exception) {
                logger.error("Failed to add theme change listener", e)
            }
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
            val titleLabel = JLabel("🔧 Select Plugin").apply {
                font = font.deriveFont(18f)
                horizontalAlignment = javax.swing.SwingConstants.CENTER
                border = javax.swing.BorderFactory.createEmptyBorder(0, 0, 20, 0)
            }

            // Description
            val descLabel = JLabel("Invalid configuration detected, please select a default plugin to continue:").apply {
                font = font.deriveFont(14f)
                horizontalAlignment = javax.swing.SwingConstants.CENTER
                border = javax.swing.BorderFactory.createEmptyBorder(0, 0, 20, 0)
            }

            // Plugin list with modern styling
            val pluginListPanel = createPluginListPanel()

            // Action buttons
            val buttonPanel = JPanel()
            buttonPanel.layout = BorderLayout()
            buttonPanel.border = javax.swing.BorderFactory.createEmptyBorder(20, 0, 0, 0)

            val debugButton = JButton("🐛 Debug Info").apply {
                preferredSize = JBUI.size(160, 36)
                font = JBFont.label().deriveFont(14f)
                isFocusPainted = false
                isOpaque = false
                addActionListener {
                    showDebugInfo()
                }
            }
            
            buttonPanel.add(debugButton, BorderLayout.WEST)
            
            // Add all components
            panel.add(titleLabel, BorderLayout.NORTH)
            panel.add(descLabel, BorderLayout.CENTER)
            panel.add(pluginListPanel, BorderLayout.CENTER)
            panel.add(buttonPanel, BorderLayout.SOUTH)
            
            return panel
        }

        /**
         * Create plugin list panel with modern styling
         */
        private fun createPluginListPanel(): JPanel {
            val panel = JPanel()
            panel.layout = javax.swing.BoxLayout(panel, javax.swing.BoxLayout.Y_AXIS)
            panel.border = javax.swing.BorderFactory.createEmptyBorder(10, 0, 10, 0)

            // Dynamically get available providers and their status
            val extensions = extensionManager.getAllExtensions()
            val currentExtensionId = ConfigFileUtils.getCurrentExtensionId()

            val plugins = extensions.map { provider ->
                val extensionId = provider.getExtensionId()
                val isCurrent = provider.getExtensionId() == currentExtensionId
                val isAvailable = provider.isAvailable(project)
                
                PluginInfo(
                    id = extensionId,
                    displayName = provider.getDisplayName(),
                    description = provider.getDescription(),
                    isAvailable = isAvailable,
                    isCurrent = isCurrent
                )
            }

            plugins.forEach { pluginInfo ->
                val pluginRow = createPluginRow(pluginInfo)
                panel.add(pluginRow)
                panel.add(javax.swing.Box.createVerticalStrut(8))
            }

            return panel
        }

        /**
         * Create a single plugin row
         */
        private fun createPluginRow(pluginInfo: PluginInfo): JPanel {
            val rowPanel = JPanel(BorderLayout())
            
            // Detect current theme for styling
            val isDarkTheme = detectCurrentTheme()
            
            // Main content panel
            val contentPanel = JPanel(BorderLayout()).apply {
                // Special background for current running plugin
                background = when {
                    pluginInfo.isCurrent -> if (isDarkTheme) {
                        java.awt.Color(0x10, 0xB9, 0x81, 0x15) // Light green background for current plugin
                    } else {
                        java.awt.Color(0x05, 0x96, 0x69, 0x10) // Light green background for current plugin
                    }
                    else -> if (isDarkTheme) {
                        java.awt.Color(0x2A, 0x2A, 0x2A, 0x80)
                    } else {
                        java.awt.Color(0xFF, 0xFF, 0xFF, 0x80)
                    }
                }
                
                // Special border for current running plugin
                val borderColor = when {
                    pluginInfo.isCurrent -> if (isDarkTheme) java.awt.Color(0x10, 0xB9, 0x81) else java.awt.Color(0x05, 0x96, 0x69)
                    else -> if (isDarkTheme) java.awt.Color(0x40, 0x40, 0x40) else java.awt.Color(0xE5, 0xE7, 0xEB)
                }
                val borderWidth = if (pluginInfo.isCurrent) 2 else 1
                
                border = BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(borderColor, borderWidth),
                    javax.swing.BorderFactory.createEmptyBorder(12, 16, 12, 16)
                )
            }

            // Top row: Title and buttons
            val topRowPanel = JPanel(BorderLayout())
            topRowPanel.isOpaque = false
            topRowPanel.border = javax.swing.BorderFactory.createEmptyBorder(0, 0, 4, 0)

            // Left side: Plugin name with status indicator
            val statusIcon = when {
                pluginInfo.isCurrent -> "🟢"
                pluginInfo.isAvailable -> "✅"
                else -> "❌"
            }
            val nameText = if (pluginInfo.isCurrent) {
                "${pluginInfo.displayName} (Currently Running)"
            } else {
                pluginInfo.displayName
            }
            val nameLabel = JLabel("$statusIcon $nameText").apply {
                font = font.deriveFont(15f).deriveFont(java.awt.Font.BOLD)
                foreground = if (pluginInfo.isAvailable) {
                    if (isDarkTheme) java.awt.Color(0xF8, 0xFA, 0xFC) else java.awt.Color(0x1E, 0x29, 0x3B)
                } else {
                    if (isDarkTheme) java.awt.Color(0x64, 0x74, 0x8B) else java.awt.Color(0x94, 0xA3, 0xB8)
                }
            }

            // Right side: Action buttons
            val buttonPanel = JPanel()
            buttonPanel.layout = javax.swing.BoxLayout(buttonPanel, javax.swing.BoxLayout.X_AXIS)
            buttonPanel.isOpaque = false

            // VSIX upload button
            val uploadButton = JButton("📦 Install From VSIX").apply {
                preferredSize = JBUI.size(160, 36)
                font = font.deriveFont(11f)
                isFocusPainted = false
                isOpaque = false
                isEnabled = true
                
                foreground = if (isDarkTheme) java.awt.Color(0xCB, 0xD5, 0xE1) else java.awt.Color(0x47, 0x56, 0x69)
                background = if (isDarkTheme) java.awt.Color(0x3E, 0x3E, 0x3E) else java.awt.Color(0xF1, 0xF5, 0xF9)
                border = BorderFactory.createEmptyBorder(4, 6, 4, 6)
                
                addActionListener {
                    uploadVsixForPlugin(pluginInfo.id, pluginInfo.displayName)
                }
            }

            buttonPanel.add(javax.swing.Box.createHorizontalStrut(8))
            buttonPanel.add(uploadButton)

            // Add title and buttons to top row
            topRowPanel.add(nameLabel, BorderLayout.WEST)
            topRowPanel.add(buttonPanel, BorderLayout.EAST)

            // Bottom row: Plugin description
            val descriptionText = if (pluginInfo.isAvailable) {
                pluginInfo.description
            } else {
                "${pluginInfo.description} (Plugin unavailable, please upload VSIX file)"
            }
            val descLabel = JLabel(descriptionText).apply {
                font = font.deriveFont(12f)
                foreground = if (pluginInfo.isAvailable) {
                    if (isDarkTheme) java.awt.Color(0xCB, 0xD5, 0xE1) else java.awt.Color(0x47, 0x56, 0x69)
                } else {
                    if (isDarkTheme) java.awt.Color(0x64, 0x74, 0x8B) else java.awt.Color(0x94, 0xA3, 0xB8)
                }
            }

            // Add components to content panel
            contentPanel.add(topRowPanel, BorderLayout.NORTH)
            contentPanel.add(descLabel, BorderLayout.CENTER)

            // Add click listener to the entire row for better UX - only for available plugins
            if (pluginInfo.isAvailable) {
                contentPanel.addMouseListener(object : java.awt.event.MouseAdapter() {
                    override fun mouseClicked(e: java.awt.event.MouseEvent) {
                        if (e.clickCount == 1) {
                            applyPluginSelection(pluginInfo.id)
                        }
                    }
                    
                    override fun mouseEntered(e: java.awt.event.MouseEvent) {
                        contentPanel.cursor = java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)
                        // Add hover effect
                        if (isDarkTheme) {
                            contentPanel.background = java.awt.Color(0x1E, 0x3A, 0x8A, 0x20)
                        } else {
                            contentPanel.background = java.awt.Color(0xDB, 0xEA, 0xFE, 0x80)
                        }
                        contentPanel.repaint()
                    }
                    
                    override fun mouseExited(e: java.awt.event.MouseEvent) {
                        contentPanel.cursor = java.awt.Cursor.getDefaultCursor()
                        // Remove hover effect
                        if (isDarkTheme) {
                            contentPanel.background = java.awt.Color(0x2A, 0x2A, 0x2A, 0x80)
                        } else {
                            contentPanel.background = java.awt.Color(0xFF, 0xFF, 0xFF, 0x80)
                        }
                        contentPanel.repaint()
                    }
                })
            } else {
                // For unavailable plugins, set default cursor and no hover effects
                contentPanel.cursor = java.awt.Cursor.getDefaultCursor()
            }

            rowPanel.add(contentPanel)
            // Prevent BoxLayout (Y_AXIS) from stretching this row vertically
            // Limit the maximum height of both contentPanel and rowPanel to their preferred heights
            val pref = contentPanel.preferredSize
            // Ensure preferred size is computed
            contentPanel.doLayout()
            val computedPref = if (pref != null && pref.height > 0) pref else contentPanel.preferredSize
            contentPanel.maximumSize = java.awt.Dimension(Int.MAX_VALUE, computedPref.height)
            rowPanel.maximumSize = java.awt.Dimension(Int.MAX_VALUE, computedPref.height)
            // Keep the row aligned to the top when extra vertical space exists
            rowPanel.alignmentY = javax.swing.Box.TOP_ALIGNMENT
            return rowPanel
        }

        /**
         * Plugin information data class
         */
        private data class PluginInfo(
            val id: String,
            val displayName: String,
            val description: String,
            val isAvailable: Boolean,
            val isCurrent: Boolean = false
        )

        /**
         * Upload VSIX file for a specific plugin
         */
        private fun uploadVsixForPlugin(pluginId: String, pluginName: String) {
            try {
                // Use VsixUploadDialog directly
                val success = VsixUploadDialog.show(project, pluginId, pluginName)
                
                if (success) {
                    javax.swing.JOptionPane.showMessageDialog(
                        contentPanel,
                        "VSIX file uploaded successfully!\nPlugin: $pluginName\nYou can now launch the plugin.",
                        "Upload Complete",
                        javax.swing.JOptionPane.INFORMATION_MESSAGE
                    )
                }
            } catch (e: Exception) {
                logger.error("Failed to upload VSIX for plugin: $pluginId", e)
                javax.swing.JOptionPane.showMessageDialog(
                    contentPanel,
                    "Upload failed: ${e.message}",
                    "Error",
                    javax.swing.JOptionPane.ERROR_MESSAGE
                )
            }
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
            // Detect current theme for status colors
            val isDarkTheme = detectCurrentTheme()
            
            if (configManager.isConfigurationLoaded()) {
                if (configManager.isConfigurationValid()) {
                    val extensionId = configManager.getCurrentExtensionId()
                    // Check if plugin is actually running
                    val isPluginRunning = isPluginActuallyRunning()
                    
                    if (isPluginRunning) {
                        statusLabel.text = "✅ Plugin Running - Current Plugin: $extensionId"
                        statusLabel.foreground = getThemeAdaptiveColor(isDarkTheme, "success")
                    } else {
                        statusLabel.text = "⚠️ Configuration Valid but Plugin Not Running - Current Plugin: $extensionId"
                        statusLabel.foreground = getThemeAdaptiveColor(isDarkTheme, "warning")
                    }
                } else {
                    statusLabel.text = "❌ Configuration Invalid - ${configManager.getConfigurationError()}"
                    statusLabel.foreground = getThemeAdaptiveColor(isDarkTheme, "error")
                }
            } else {
                statusLabel.text = "⏳ Loading Configuration..."
                statusLabel.foreground = getThemeAdaptiveColor(isDarkTheme, "info")
            }
        }

        /**
         * Get theme-adaptive color for status indicators
         */
        private fun getThemeAdaptiveColor(isDarkTheme: Boolean, colorType: String): java.awt.Color {
            return if (isDarkTheme) {
                when (colorType) {
                    "success" -> java.awt.Color(16, 185, 129) // Green for dark theme
                    "warning" -> java.awt.Color(251, 191, 36) // Yellow for dark theme
                    "error" -> java.awt.Color(239, 68, 68)   // Red for dark theme
                    "info" -> java.awt.Color(59, 130, 246)   // Blue for dark theme
                    else -> java.awt.Color(148, 163, 184)    // Default gray for dark theme
                }
            } else {
                when (colorType) {
                    "success" -> java.awt.Color(5, 150, 105)  // Green for light theme
                    "warning" -> java.awt.Color(217, 119, 6)  // Yellow for light theme
                    "error" -> java.awt.Color(220, 38, 38)    // Red for light theme
                    "info" -> java.awt.Color(37, 99, 235)     // Blue for light theme
                    else -> java.awt.Color(100, 116, 139)     // Default gray for light theme
                }
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
                    // Start the plugin directly instead of just saving configuration
                    startPluginAfterSelection(pluginId)
                    
                    logger.info("Plugin selection applied successfully: $pluginId")
                } else {
                    // Configuration is still invalid after setting
                    val errorMsg = configManager.getConfigurationError() ?: "Unknown error"
                    val message = "❌ Configuration Update Failed\nError: $errorMsg\n\nPlease check the configuration file or try manual configuration."
                    javax.swing.JOptionPane.showMessageDialog(
                        contentPanel,
                        message,
                        "Configuration Update Failed",
                        javax.swing.JOptionPane.ERROR_MESSAGE
                    )
                    
                    logger.error("Configuration is still invalid after setting extension ID: $pluginId, error: $errorMsg")
                }
            } catch (e: Exception) {
                logger.error("Failed to apply plugin selection", e)
                val message = "❌ Configuration Update Failed\nError: ${e.message}\n\nPlease check file permissions or try manual configuration."
                javax.swing.JOptionPane.showMessageDialog(
                    contentPanel,
                    message,
                    "Error",
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
                        SystemObjectProvider.dispose(project)
                        // Reset state when disposing
                        isPluginRunning = false
                        isPluginStarting = false
                    })
                    
                    logger.info("Plugin started successfully after selection: $pluginId")
                    
                    // Set plugin running state
                    isPluginRunning = true
                    isPluginStarting = false
                    
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
                
                val message = "❌ Plugin Startup Failed\nError: ${e.message}\n\nPlease check plugin configuration or try restarting the IDE."
                javax.swing.JOptionPane.showMessageDialog(
                    contentPanel,
                    message,
                    "Plugin Startup Failed",
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
                📝 Manual Configuration Instructions
                
                1. Create configuration file in user home directory: ${PluginConstants.ConfigFiles.getMainConfigPath()}
                2. Add the following content:
                   ${PluginConstants.ConfigFiles.EXTENSION_TYPE_KEY}=roo-code
                   
                3. Supported plugin types:
                   - roo-code: Roo Code AI Assistant
                   - cline: Cline AI Assistant
                   - custom: Custom Plugin
                   
                4. Save the file and restart IDE
                
                Configuration file path: ${configManager.getConfigurationFilePath()}
            """.trimIndent()
            
            javax.swing.JOptionPane.showMessageDialog(
                contentPanel,
                instructions,
                "Manual Configuration Instructions",
                javax.swing.JOptionPane.INFORMATION_MESSAGE
            )
        }

        /**
         * Show debug information
         */
        private fun showDebugInfo() {
            val debugText = """
                RunVSAgent Debug Information
                ============================
                
                🚀 Plugin Status: ${if (configManager.isConfigurationLoaded() && configManager.isConfigurationValid()) "Loaded and Valid" else "Not Loaded or Invalid"}
                
                📝 Current Configuration: ${configManager.getCurrentExtensionId() ?: "Not Set"}
                
                ⚙️ Configuration File Path: ${configManager.getConfigurationFilePath()}
                
                🔄 Configuration Load Time: ${configManager.getConfigurationLoadTime()?.let { it.toString() } ?: "Unknown"}
                
                💡 Tip: If configuration is invalid, please check the configuration file content or try manual configuration.
            """.trimIndent()
            
            javax.swing.JOptionPane.showMessageDialog(
                contentPanel,
                debugText,
                "Debug Information",
                javax.swing.JOptionPane.INFORMATION_MESSAGE
            )
        }
    }
}