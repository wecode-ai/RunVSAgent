// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.extensions.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.io.File
import javax.swing.*
import javax.swing.event.ListSelectionListener
import com.sina.weibo.agent.extensions.core.ExtensionManager
import com.sina.weibo.agent.extensions.core.ExtensionSwitcher
import com.sina.weibo.agent.extensions.core.ExtensionConfigurationManager
import com.sina.weibo.agent.extensions.core.VsixManager
import com.sina.weibo.agent.extensions.config.ExtensionProvider
import com.sina.weibo.agent.util.PluginResourceUtil
import com.sina.weibo.agent.util.PluginConstants

/**
 * Extension switcher dialog
 * Provides UI for switching between different extension providers
 */
class ExtensionSwitcherDialog(private val project: Project) : DialogWrapper(project) {

    // region --- Theme tokens & helpers --------------------------------------

    /** 语义化颜色令牌，统一从当前 Look & Feel / UIManager 派生 */
    private object ColorTokens {
        val panelBg get() = UIManager.getColor("Panel.background") ?: JBColor.PanelBackground
        val panelBorder get() = UIManager.getColor("Component.borderColor") ?: JBColor.border()
        val separator get() = UIManager.getColor("Separator.foreground") ?: JBColor.border()

        val textPrimary get() = UIManager.getColor("Label.foreground") ?: JBColor.foreground()
        val textSecondary get() = UIManager.getColor("Component.infoForeground") ?: JBColor.GRAY

        val listBg get() = UIManager.getColor("List.background") ?: UIUtil.getListBackground()
        val listFg get() = UIManager.getColor("List.foreground") ?: UIUtil.getListForeground()
        val listSelBg get() = UIManager.getColor("List.selectionBackground")
            ?: UIUtil.getListSelectionBackground(true)
        val listSelFg get() = UIManager.getColor("List.selectionForeground")
            ?: UIUtil.getListSelectionForeground(true)

        // 标签/小徽章背景与边框（当前项）
        val tagBgCurrent get() = if (UIUtil.isUnderDarcula())
            javax.swing.plaf.ColorUIResource(java.awt.Color(0x2D, 0x5A, 0xA0, 0x33))
        else JBColor(0xE6F0FF, 0x2D5AA0)

        val tagBorderCurrent get() = JBColor(0xA9C7FF, 0x1E3A5F)

        // 普通标签背景
        val tagBgNormal get() = UIManager.getColor("TextField.inactiveBackground")
            ?: JBColor(0xF4F4F4, 0x3E3E3E)

        // 轻度强调边框（当前扩展列表项）
        val accentBorder get() = JBColor(0x6AA7FF, 0x6AA7FF)

        // 代码块/路径背景
        val codeBg get() = UIManager.getColor("EditorPane.inactiveBackground")
            ?: JBColor(0xF5F5F5, 0x3A3A3A)

        // 警告卡片背景
        val warningBg get() = UIManager.getColor("Notification.warningBackground")
            ?: JBColor(0xFFF4E5, 0x5A3E22)
    }

    /** 常用边距/线框 */
    private object Ui {
        fun insets(t: Int = 8, l: Int = 8, b: Int = 8, r: Int = 8) = JBUI.Borders.empty(t, l, b, r)
        fun line(color: java.awt.Color = ColorTokens.panelBorder, thickness: Int = 1) =
            JBUI.Borders.customLine(color, thickness)
    }

    /** 将 AWT Color 转为 CSS 十六进制 */
    private fun toCss(c: java.awt.Color) = String.format("#%02x%02x%02x", c.red, c.green, c.blue)

    // endregion ---------------------------------------------------------------

    private val extensionManager = ExtensionManager.getInstance(project)
    private val extensionSwitcher = ExtensionSwitcher.getInstance(project)
    private val configManager = ExtensionConfigurationManager.getInstance(project)

    // UI components
    private lateinit var extensionList: JBList<ExtensionItem>
    private lateinit var descriptionLabel: JBLabel
    private lateinit var statusLabel: JBLabel
    private lateinit var resourceStatusLabel: JBLabel
    private lateinit var uploadVsixButton: JButton
    private lateinit var autoSwitchCheckBox: JBCheckBox
    private lateinit var switchButton: JButton
    private lateinit var cancelButton: JButton

    // Data
    private val extensionItems = mutableListOf<ExtensionItem>()
    private var selectedExtensionId: String? = null

    init {
        title = "Switch Extension Provider"
        init()
        loadExtensions()
        updateUI()
    }

    /**
     * Extension item for list display
     */
    private data class ExtensionItem(
        val id: String,
        val displayName: String,
        val description: String,
        val isAvailable: Boolean,
        val isCurrent: Boolean,
        val resourceStatus: ResourceStatus
    )

    /**
     * Resource status information
     */
    private data class ResourceStatus(
        val projectResourceExists: Boolean,
        val projectResourcePath: String?,
        val pluginResourceExists: Boolean,
        val pluginResourcePath: String?,
        val vsixResourceExists: Boolean,
        val vsixResourcePath: String?,
        val statusText: String,
        val statusIcon: String
    )

    /**
     * Create center panel
     */
    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout()).apply {
            preferredSize = Dimension(800, 600)
            background = ColorTokens.panelBg
        }

        // Extension list
        extensionList = JBList<ExtensionItem>().apply {
            selectionMode = ListSelectionModel.SINGLE_SELECTION
            addListSelectionListener(createListSelectionListener())

            // 自定义渲染器：使用系统 List 配色与强调边框
            setCellRenderer { _, value, _, isSelected, _ ->
                val label = JBLabel().apply {
                    isOpaque = true
                    background = if (isSelected) ColorTokens.listSelBg else ColorTokens.listBg
                    foreground = if (isSelected) ColorTokens.listSelFg else ColorTokens.listFg
                    border = Ui.insets(4, 6, 4, 6)
                }
                if (value is ExtensionItem) {
                    val resourceIcon = when {
                        value.resourceStatus.projectResourceExists -> "🟢"
                        value.resourceStatus.vsixResourceExists -> "🔵"
                        value.resourceStatus.pluginResourceExists -> "🟡"
                        else -> "🔴"
                    }
                    val displayText = if (value.isCurrent)
                        "<b>${value.displayName}</b> <i>(Current)</i>"
                    else value.displayName

                    label.text = "<html><div style='padding:2px;'>$resourceIcon $displayText</div></html>"

                    // tooltip 里的 code 背景令牌化
                    fun code(path: String?) =
                        "<code style='background:${toCss(ColorTokens.codeBg)};padding:2px 4px;border-radius:2px;'>$path</code>"

                    label.toolTipText = buildString {
                        append("<html><div style='padding:5px;'>")
                        append("<b>Extension: ${value.displayName}</b><br><br>")
                        append("<b>Status: ${value.resourceStatus.statusText}</b><br><br>")
                        if (value.resourceStatus.projectResourceExists) {
                            append("📁 <b>Local Path:</b><br>${code(value.resourceStatus.projectResourcePath)}<br><br>")
                        }
                        if (value.resourceStatus.vsixResourceExists) {
                            append("📦 <b>VSIX Path:</b><br>${code(value.resourceStatus.vsixResourcePath)}<br><br>")
                        }
                        if (value.resourceStatus.pluginResourceExists) {
                            append("🔧 <b>Plugin Path:</b><br>${code(value.resourceStatus.pluginResourcePath)}")
                        }
                        if (!value.resourceStatus.projectResourceExists &&
                            !value.resourceStatus.vsixResourceExists &&
                            !value.resourceStatus.pluginResourceExists
                        ) {
                            append("<br>⚠️ <b>Warning: No resource files found!</b>")
                        }
                        append("</div></html>")
                    }

                    // 当前扩展强调边框（不改动选中色）
                    val outer = if (value.isCurrent) Ui.line(ColorTokens.accentBorder, 2) else Ui.insets(0)
                    label.border = BorderFactory.createCompoundBorder(outer, Ui.insets(2, 2, 2, 2))
                }
                label
            }
        }

        val listScrollPane = JScrollPane(extensionList).apply {
            preferredSize = Dimension(350, 400)
            background = ColorTokens.panelBg
        }

        // Right panel for details
        val rightPanel = createRightPanel()

        // Main layout with proper spacing
        panel.add(createTopPanel(), BorderLayout.NORTH)
        panel.add(createMainContentPanel(listScrollPane, rightPanel), BorderLayout.CENTER)
        panel.add(createBottomPanel(), BorderLayout.SOUTH)

        return panel
    }

    /**
     * Create main content panel with proper layout
     */
    private fun createMainContentPanel(listScrollPane: JScrollPane, rightPanel: JPanel): JPanel {
        val panel = JPanel(BorderLayout()).apply {
            border = Ui.insets(10, 10, 10, 10)
            background = ColorTokens.panelBg
        }

        fun titled(title: String, content: JComponent) = JPanel(BorderLayout()).apply {
            background = ColorTokens.panelBg
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                    Ui.line(ColorTokens.panelBorder),
                    title,
                    javax.swing.border.TitledBorder.LEFT,
                    javax.swing.border.TitledBorder.TOP
                ),
                Ui.insets(5, 5, 5, 5)
            )
            add(content, BorderLayout.CENTER)
        }

        val leftPanel = titled("Available Extensions", listScrollPane)
        val rightPanelWithBorder = titled("Extension Details", rightPanel)

        panel.add(leftPanel, BorderLayout.WEST)
        panel.add(Box.createHorizontalStrut(JBUI.scale(15)), BorderLayout.CENTER)
        panel.add(rightPanelWithBorder, BorderLayout.EAST)

        return panel
    }

    /**
     * Create top panel
     */
    private fun createTopPanel(): JPanel {
        val panel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createCompoundBorder(
                Ui.insets(15, 15, 15, 15),
                JBUI.Borders.customLine(ColorTokens.separator, 0, 0, 1, 0)
            )
            background = ColorTokens.panelBg
        }

        val titleLabel = JBLabel("Select Extension Provider").apply {
            font = font.deriveFont(font.size + 2f)
            foreground = ColorTokens.textPrimary
            alignmentX = Component.LEFT_ALIGNMENT
        }
        panel.add(titleLabel)

        panel.add(Box.createVerticalStrut(JBUI.scale(12)))

        val legendText =
            "<html><div style='white-space: nowrap;'><b>Resource Status Legend:</b><br>🟢 Local Resources | 🔵 VSIX Installation | 🟡 Built-in Resources | 🔴 No Resources</div></html>"
        val legendLabel = JBLabel(legendText).apply {
            foreground = ColorTokens.textSecondary
            preferredSize = Dimension(JBUI.scale(600), JBUI.scale(40))
            alignmentX = Component.LEFT_ALIGNMENT
        }
        panel.add(legendLabel)

        return panel
    }

    /**
     * Create right panel
     */
    private fun createRightPanel(): JPanel {
        val panel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = Ui.insets(10, 10, 10, 10)
            background = ColorTokens.panelBg
        }

        // Description section
        val descriptionPanel = createSectionPanel("Description")
        descriptionLabel = JBLabel("Select an extension to view details").apply {
            preferredSize = Dimension(350, 80)
            alignmentX = Component.LEFT_ALIGNMENT
            foreground = ColorTokens.textPrimary
        }
        descriptionPanel.add(descriptionLabel)
        panel.add(descriptionPanel)

        panel.add(Box.createVerticalStrut(JBUI.scale(15)))

        // Status section
        val statusPanel = createSectionPanel("Status")
        statusLabel = JBLabel("").apply {
            preferredSize = Dimension(350, 30)
            alignmentX = Component.LEFT_ALIGNMENT
            foreground = ColorTokens.textPrimary
        }
        statusPanel.add(statusLabel)
        panel.add(statusPanel)

        panel.add(Box.createVerticalStrut(JBUI.scale(15)))

        // Resource Status section
        val resourceStatusPanel = createSectionPanel("Resource Status")
        resourceStatusLabel = JBLabel("").apply {
            preferredSize = Dimension(350, 120)
            alignmentX = Component.LEFT_ALIGNMENT
            foreground = ColorTokens.textPrimary
        }
        resourceStatusPanel.add(resourceStatusLabel)
        panel.add(resourceStatusPanel)

        panel.add(Box.createVerticalStrut(JBUI.scale(20)))

        // Upload VSIX Button（保持默认 LAF 外观，不自定义前景背景）
        val uploadButtonPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            background = ColorTokens.panelBg
        }
        uploadVsixButton = JButton("Select Extension First").apply {
            preferredSize = JBUI.size(180, 35)
            isEnabled = false
            toolTipText = "Please select an extension to upload VSIX file"
            addActionListener { uploadVsixFile() }
        }
        uploadButtonPanel.add(uploadVsixButton)
        uploadButtonPanel.alignmentX = Component.LEFT_ALIGNMENT
        panel.add(uploadButtonPanel)

        panel.add(Box.createVerticalStrut(JBUI.scale(15)))

        // Auto-switch option (not implemented yet)
        val checkboxPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            background = ColorTokens.panelBg
        }
        autoSwitchCheckBox = JBCheckBox("Remember this choice for future projects").apply {
            isSelected = false
            isEnabled = false
        }
        checkboxPanel.add(autoSwitchCheckBox)
        checkboxPanel.alignmentX = Component.LEFT_ALIGNMENT
        panel.add(checkboxPanel)

        panel.add(Box.createVerticalGlue())

        return panel
    }

    /**
     * Create a section panel with title
     */
    private fun createSectionPanel(title: String): JPanel {
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = ColorTokens.panelBg
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                    Ui.line(ColorTokens.panelBorder),
                    title,
                    javax.swing.border.TitledBorder.LEFT,
                    javax.swing.border.TitledBorder.TOP
                ),
                Ui.insets(8, 8, 8, 8)
            )
            alignmentX = Component.LEFT_ALIGNMENT
        }
    }

    /**
     * Create bottom panel
     */
    private fun createBottomPanel(): JPanel {
        val panel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            border = BorderFactory.createCompoundBorder(
                JBUI.Borders.customLine(ColorTokens.separator, 1, 0, 0, 0),
                Ui.insets(15, 15, 15, 15)
            )
            background = ColorTokens.panelBg
        }

        panel.add(Box.createHorizontalGlue())

        // Switch button（默认外观）
        switchButton = JButton("Switch Extension").apply {
            preferredSize = JBUI.size(120, 35)
            isEnabled = false
            addActionListener { performSwitch() }
        }
        panel.add(switchButton)

        panel.add(Box.createHorizontalStrut(JBUI.scale(10)))

        // Cancel button（默认外观）
        cancelButton = JButton("Cancel").apply {
            preferredSize = JBUI.size(100, 35)
            addActionListener { doCancelAction() }
        }
        panel.add(cancelButton)
        return panel
    }

    /**
     * Create list selection listener
     */
    private fun createListSelectionListener(): ListSelectionListener {
        return ListSelectionListener { e ->
            if (!e.valueIsAdjusting) {
                val selectedIndex = extensionList.selectedIndex
                if (selectedIndex >= 0) {
                    val selectedItem = extensionItems[selectedIndex]
                    selectedExtensionId = selectedItem.id
                    updateDescription(selectedItem)
                    updateSwitchButton()
                }
            }
        }
    }

    /**
     * Load available extensions
     */
    private fun loadExtensions() {
        extensionItems.clear()

        val currentProvider = extensionManager.getCurrentProvider()
        val availableProviders = extensionManager.getAvailableProviders()

        availableProviders.forEach { provider ->
            val resourceStatus = checkExtensionResourceStatus(provider, project)
            val item = ExtensionItem(
                id = provider.getExtensionId(),
                displayName = provider.getDisplayName(),
                description = provider.getDescription(),
                isAvailable = provider.isAvailable(project),
                isCurrent = provider == currentProvider,
                resourceStatus = resourceStatus
            )
            extensionItems.add(item)
        }

        extensionList.setListData(extensionItems.toTypedArray())

        // Select current extension
        val currentIndex = extensionItems.indexOfFirst { it.isCurrent }
        if (currentIndex >= 0) {
            extensionList.selectedIndex = currentIndex
        }
    }

    /**
     * Update description display
     */
    private fun updateDescription(item: ExtensionItem) {
        descriptionLabel.text = """
          <html>
            <div style='margin:5px;'>
              <div style='font-size:14px;font-weight:bold;margin-bottom:8px;'>${item.displayName}</div>
              <div style='line-height:1.4;'>${item.description}</div>
            </div>
          </html>
        """.trimIndent()

        val (bg, border) = if (item.isCurrent) {
            ColorTokens.tagBgCurrent to ColorTokens.tagBorderCurrent
        } else {
            ColorTokens.tagBgNormal to ColorTokens.panelBorder
        }

        val statusText = when {
            item.isCurrent -> "Currently active"
            !item.isAvailable -> "Not available"
            else -> "Available"
        }

        statusLabel.text = """
          <html>
            <div style='margin:5px;'>
              <span style='padding:4px 8px;border-radius:12px;font-size:12px;font-weight:bold;
                           background-color:${toCss(bg)};border:1px solid ${toCss(border)};'>
                $statusText
              </span>
            </div>
          </html>
        """.trimIndent()

        // Update resource status display
        updateResourceStatusDisplay(item.resourceStatus)

        // Update upload button state
        updateUploadButtonState(item.resourceStatus)
    }

    /**
     * Update upload button state based on resource status
     * Always allow re-uploading even if resources exist
     */
    private fun updateUploadButtonState(resourceStatus: ResourceStatus) {
        val hasResources = resourceStatus.projectResourceExists ||
                resourceStatus.vsixResourceExists ||
                resourceStatus.pluginResourceExists

        uploadVsixButton.isVisible = true
        uploadVsixButton.isEnabled = true

        if (hasResources) {
            uploadVsixButton.text = "Re-upload VSIX File"
            uploadVsixButton.toolTipText = "Upload new VSIX file to update/replace existing extension resources"
        } else {
            uploadVsixButton.text = "Upload VSIX File"
            uploadVsixButton.toolTipText = "Upload VSIX file to install extension resources"
        }
    }

    /**
     * Upload VSIX file for the selected extension
     */
    private fun uploadVsixFile() {
        val selectedItem = selectedExtensionId?.let { id ->
            extensionItems.find { it.id == id }
        }

        if (selectedItem == null) {
            Messages.showWarningDialog(
                "Please select an extension first.",
                "No Extension Selected"
            )
            return
        }

        val success = VsixUploadDialog.show(
            project,
            selectedItem.id,
            selectedItem.displayName
        )

        if (success) {
            // Refresh the extension list to show updated status
            loadExtensions()
            updateUI()

            // Show success message
            Messages.showInfoMessage(
                "VSIX file uploaded successfully for ${selectedItem.displayName}.\n" +
                        "The extension should now be available.",
                "Upload Complete"
            )
        }
    }

    /**
     * Check extension resource status
     */
    private fun checkExtensionResourceStatus(provider: ExtensionProvider, project: Project): ResourceStatus {
        val extensionConfig = provider.getConfiguration(project)

        // Check project paths first
        val projectPath = project.basePath
        var projectResourceExists = false
        var projectResourcePath: String? = null

        if (projectPath != null) {
            val possiblePaths = listOf(
                "$projectPath/${extensionConfig.getCodeDir()}",
                "$projectPath/../${extensionConfig.getCodeDir()}",
                "$projectPath/../../${extensionConfig.getCodeDir()}"
            )

            for (path in possiblePaths) {
                if (File(path).exists()) {
                    projectResourceExists = true
                    projectResourcePath = path
                    break
                }
            }
        }

        // Check plugin resources
        var pluginResourceExists = false
        var pluginResourcePath: String? = null

        try {
            val pluginPath = PluginResourceUtil.getResourcePath(
                PluginConstants.PLUGIN_ID,
                extensionConfig.getCodeDir()
            )
            if (pluginPath != null && File(pluginPath).exists()) {
                pluginResourceExists = true
                pluginResourcePath = pluginPath
            }
        } catch (_: Exception) {
            // Ignore exceptions when checking plugin resources
        }

        // Check VSIX installation
        val vsixManager = VsixManager.getInstance()
        val extensionId = provider.getExtensionId()
        val vsixResourceExists = vsixManager.hasVsixInstallation(extensionId)
        val vsixResourcePath = if (vsixResourceExists) {
            vsixManager.getVsixInstallationPath(extensionId)
        } else null

        // Determine status text and icon
        val (statusText, statusIcon) = when {
            projectResourceExists -> "✓ Local Resources Found" to "🟢"
            vsixResourceExists -> "✓ VSIX Installation Found" to "🔵"
            pluginResourceExists -> "✓ Built-in Resources Found" to "🟡"
            else -> "⚠ No Resources Found" to "🔴"
        }

        return ResourceStatus(
            projectResourceExists = projectResourceExists,
            projectResourcePath = projectResourcePath,
            pluginResourceExists = pluginResourceExists,
            pluginResourcePath = pluginResourcePath,
            vsixResourceExists = vsixResourceExists,
            vsixResourcePath = vsixResourcePath,
            statusText = statusText,
            statusIcon = statusIcon
        )
    }

    /**
     * Update resource status display
     */
    private fun updateResourceStatusDisplay(resourceStatus: ResourceStatus) {
        val pillBg = ColorTokens.tagBgNormal
        val codeBg = ColorTokens.codeBg
        resourceStatusLabel.text = buildString {
            append("<html><div style='margin:5px;'>")

            // Main status with icon
            append(
                "<div style='margin-bottom:10px;padding:8px;background:${toCss(pillBg)};" +
                        "border-radius:4px;'><b>${resourceStatus.statusIcon} ${resourceStatus.statusText}</b></div>"
            )

            // Resource details
            if (resourceStatus.projectResourceExists) {
                append("<div style='margin-bottom:8px;'><b>📁 Local Path:</b><br>")
                append("<code style='background:${toCss(codeBg)};padding:2px 4px;border-radius:2px;'>${resourceStatus.projectResourcePath}</code></div>")
            }
            if (resourceStatus.vsixResourceExists) {
                append("<div style='margin-bottom:8px;'><b>📦 VSIX Installation:</b><br>")
                append("<code style='background:${toCss(codeBg)};padding:2px 4px;border-radius:2px;'>${resourceStatus.vsixResourcePath}</code></div>")
            }
            if (resourceStatus.pluginResourceExists) {
                append("<div style='margin-bottom:8px;'><b>🔧 Plugin Path:</b><br>")
                append("<code style='background:${toCss(codeBg)};padding:2px 4px;border-radius:2px;'>${resourceStatus.pluginResourcePath}</code></div>")
            }

            if (!resourceStatus.projectResourceExists &&
                !resourceStatus.vsixResourceExists &&
                !resourceStatus.pluginResourceExists
            ) {
                append(
                    "<div style='margin-top:10px;padding:8px;background:${toCss(ColorTokens.warningBg)};" +
                            "border-radius:4px;'><b>⚠️ Warning: No resource files found!</b><br>" +
                            "This extension may not function properly.<br>" +
                            "You can upload a VSIX file to install the extension.</div>"
                )
            }

            append("</div></html>")
        }
    }

    /**
     * Update switch button state
     */
    private fun updateSwitchButton() {
        val selectedItem = selectedExtensionId?.let { id ->
            extensionItems.find { it.id == id }
        }

        // Allow switching even if the extension doesn't have resources
        // The user can upload VSIX after switching
        switchButton.isEnabled = selectedItem != null &&
                selectedItem.isAvailable &&
                !selectedItem.isCurrent &&
                !extensionSwitcher.isSwitching()

        // Update button text to indicate VSIX upload possibility
        if (selectedItem != null && !selectedItem.isCurrent) {
            val hasResources =
                selectedItem.resourceStatus.projectResourceExists ||
                        selectedItem.resourceStatus.vsixResourceExists ||
                        selectedItem.resourceStatus.pluginResourceExists

            if (hasResources) {
                switchButton.text = "Switch Extension"
                switchButton.toolTipText = "Switch to ${selectedItem.displayName}"
            } else {
                switchButton.text = "Switch & Upload VSIX"
                switchButton.toolTipText =
                    "Switch to ${selectedItem.displayName} and upload VSIX file for resources"
            }
        } else {
            switchButton.text = "Switch Extension"
            switchButton.toolTipText = "Select an extension to switch to"
        }
    }

    /**
     * Update UI state
     */
    private fun updateUI() {
        if (extensionSwitcher.isSwitching()) {
            switchButton.text = "Switching..."
            switchButton.isEnabled = false
            cancelButton.isEnabled = false
        } else {
            switchButton.text = "Switch Extension"
            updateSwitchButton()
            cancelButton.isEnabled = true
        }

        // Update upload button state for selected item
        val selectedItem = selectedExtensionId?.let { id ->
            extensionItems.find { it.id == id }
        }
        if (selectedItem != null) {
            updateUploadButtonState(selectedItem.resourceStatus)
        } else {
            // No selection, show upload button but disable it
            uploadVsixButton.isVisible = true
            uploadVsixButton.isEnabled = false
            uploadVsixButton.text = "Select Extension First"
            uploadVsixButton.toolTipText = "Please select an extension to upload VSIX file"
        }
    }

    /**
     * Perform extension switch
     */
    private fun performSwitch() {
        val targetExtensionId = selectedExtensionId ?: return

        val currentProvider = extensionManager.getCurrentProvider()
        val currentExtensionId = currentProvider?.getExtensionId()

        if (currentExtensionId == targetExtensionId) {
            Messages.showInfoMessage("Already using this extension", "No Change Needed")
            return
        }

        // Confirm switch
        val result = Messages.showYesNoDialog(
            "Are you sure you want to switch from '$currentExtensionId' to '$targetExtensionId'?\n\n" +
                    "This will restart the extension process and may cause a brief interruption.",
            "Confirm Extension Switch",
            "Switch",
            "Cancel",
            Messages.getQuestionIcon()
        )

        if (result == Messages.YES) {
            // Update configuration
            configManager.setCurrentExtensionId(targetExtensionId)
            // Note: Auto-switch functionality not implemented yet
            // configManager.setAutoSwitchEnabled(autoSwitchCheckBox.isSelected)

            // Perform switch
            performExtensionSwitch(targetExtensionId)
        }
    }

    /**
     * Perform the actual extension switch
     */
    private fun performExtensionSwitch(targetExtensionId: String) {
        switchButton.text = "Switching..."
        switchButton.isEnabled = false
        cancelButton.isEnabled = false

        // Start switching in background
        extensionSwitcher.switchExtension(targetExtensionId, forceRestart = true)
            .whenComplete { success: Boolean, throwable: Throwable? ->
                SwingUtilities.invokeLater {
                    if (success) {
                        Messages.showInfoMessage(
                            "Successfully switched to extension: $targetExtensionId",
                            "Extension Switch Complete"
                        )
                        close(OK_EXIT_CODE)
                    } else {
                        val errorMessage = throwable?.message ?: "Unknown error occurred"
                        Messages.showErrorDialog(
                            "Failed to switch extension: $errorMessage",
                            "Extension Switch Failed"
                        )

                        // Reset UI
                        switchButton.text = "Switch Extension"
                        updateSwitchButton()
                        cancelButton.isEnabled = true
                    }
                }
            }
    }

    /**
     * Override cancel action
     */
    override fun doCancelAction() {
        if (extensionSwitcher.isSwitching()) {
            val result = Messages.showYesNoDialog(
                "Extension switching is in progress. Are you sure you want to cancel?",
                "Cancel Extension Switch",
                "Cancel Switch",
                "Wait",
                Messages.getQuestionIcon()
            )

            if (result == Messages.YES) {
                extensionSwitcher.cancelSwitching()
                super.doCancelAction()
            }
        } else {
            super.doCancelAction()
        }
    }
}