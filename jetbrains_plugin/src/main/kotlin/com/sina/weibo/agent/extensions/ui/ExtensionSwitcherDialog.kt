// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.extensions.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.*
import java.awt.datatransfer.StringSelection
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
 * Simplified Extension Switcher Dialog
 */
class ExtensionSwitcherDialog(private val project: Project) : DialogWrapper(project) {

    // === State & services ====================================================================
    private val extensionManager = ExtensionManager.getInstance(project)
    private val extensionSwitcher = ExtensionSwitcher.getInstance(project)
    private val configManager = ExtensionConfigurationManager.getInstance(project)

    private lateinit var extensionList: JBList<ExtensionListItem>
    private lateinit var descriptionLabel: JBLabel
    private lateinit var statusLabel: JBLabel
    private lateinit var switchButton: JButton
    private lateinit var setAsDefaultCheckBox: JBCheckBox
    private lateinit var installButton: JButton
    private lateinit var refreshButton: JButton

    private val extensionListItems = mutableListOf<ExtensionListItem>()
    private var selectedExtensionId: String? = null
    private var isSwitching = false

    init {
        title = "Switch Extension Provider"
        init()
        loadExtensions()
        setupUI()
    }

    // === Data models =========================================================================
    private data class ExtensionListItem(
        val id: String,
        val displayName: String,
        val description: String,
        val isAvailable: Boolean,
        val isCurrent: Boolean,
        val resourceStatus: ResourceStatus
    )

    private data class ResourceStatus(
        val projectResourceExists: Boolean,
        val projectResourcePath: String?,
        val pluginResourceExists: Boolean,
        val pluginResourcePath: String?,
        val vsixResourceExists: Boolean,
        val vsixResourcePath: String?,
    )

    // === UI build =============================================================================
    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout()).apply {
            preferredSize = Dimension(600, 400)
            border = JBUI.Borders.empty(16)
        }

        // Left side - Extension list
        val leftPanel = JPanel(BorderLayout()).apply {
            preferredSize = Dimension(300, 0)
            border = BorderFactory.createTitledBorder("Extensions")
        }

        extensionList = JBList<ExtensionListItem>().apply {
            selectionMode = ListSelectionModel.SINGLE_SELECTION
            setCellRenderer(listCellRenderer())
            addListSelectionListener(listSelectionListener())
        }
        leftPanel.add(JScrollPane(extensionList), BorderLayout.CENTER)

        // Right side - Details and actions
        val rightPanel = JPanel(BorderLayout()).apply {
            border = BorderFactory.createTitledBorder("Details")
            preferredSize = Dimension(270, 0)
            minimumSize = Dimension(270, 0)
            maximumSize = Dimension(270, Int.MAX_VALUE)
        }

        val detailsPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = JBUI.Borders.empty(16)
        }

        descriptionLabel = JBLabel("Select an extension to view details")
        statusLabel = JBLabel("")
        switchButton = JButton("Switch").apply { 
            isEnabled = false
            addActionListener { performSwitch() }
        }
        setAsDefaultCheckBox = JBCheckBox("Set as default").apply { 
            isEnabled = false 
        }

        detailsPanel.add(descriptionLabel)
        detailsPanel.add(Box.createVerticalStrut(8))
        detailsPanel.add(statusLabel)
        detailsPanel.add(Box.createVerticalStrut(16))
        detailsPanel.add(switchButton)
        detailsPanel.add(Box.createVerticalStrut(8))
//        detailsPanel.add(setAsDefaultCheckBox)

        rightPanel.add(detailsPanel, BorderLayout.CENTER)

        // Bottom buttons
        val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        installButton = JButton("Install VSIX").apply { addActionListener { uploadVsixFile() } }
        refreshButton = JButton("Refresh").apply { addActionListener { loadExtensions() } }
        buttonPanel.add(installButton)
        buttonPanel.add(refreshButton)

        rightPanel.add(buttonPanel, BorderLayout.SOUTH)

        panel.add(leftPanel, BorderLayout.WEST)
        panel.add(Box.createHorizontalStrut(16), BorderLayout.CENTER)
        panel.add(rightPanel, BorderLayout.EAST)

        return panel
    }

    private fun setupUI() {
        // Set initial state
        updateUI(null)
    }

    // === List rendering ======================================================================
    private fun listCellRenderer(): ListCellRenderer<ExtensionListItem> = ListCellRenderer { _, value, _, isSelected, _ ->
        JPanel(BorderLayout()).apply {
            isOpaque = true
            background = if (isSelected) UIUtil.getListSelectionBackground(true) else UIUtil.getListBackground()
            border = JBUI.Borders.empty(4, 8)
            
            val nameLabel = JBLabel(value.displayName).apply {
                foreground = if (isSelected) UIUtil.getListSelectionForeground(true) else UIUtil.getListForeground()
                if (value.isCurrent) font = font.deriveFont(Font.BOLD)
            }
            
            val statusLabel = JBLabel(statusText(value)).apply {
                foreground = Color.WHITE
                background = statusColor(value)
                border = JBUI.Borders.empty(2, 6)
                isOpaque = true
            }
            
            add(nameLabel, BorderLayout.WEST)
            add(statusLabel, BorderLayout.EAST)
        }
    }

    private fun listSelectionListener(): ListSelectionListener = ListSelectionListener { e ->
        if (!e.valueIsAdjusting) {
            val idx = extensionList.selectedIndex
            if (idx >= 0) {
                val item = extensionList.model.getElementAt(idx) as ExtensionListItem
                selectedExtensionId = item.id
                updateUI(item)
            } else {
                selectedExtensionId = null
                updateUI(null)
            }
        }
    }

    private fun statusText(item: ExtensionListItem): String = getExtensionStatus(item).status

    private fun statusColor(item: ExtensionListItem): Color = when (getExtensionStatus(item).status) {
        "Current" -> JBColor.GREEN
        "Next Startup" -> JBColor.BLUE
        "Uninstalled" -> JBColor.RED
        "Installed" -> JBColor.BLUE
        else -> JBColor.ORANGE
    }

    // === Behavior ============================================================================
    
    /**
     * Get extension status information
     */
    private data class ExtensionStatus(
        val status: String,
        val displayText: String,
        val icon: String,
        val buttonText: String
    )
    
    private fun getExtensionStatus(item: ExtensionListItem): ExtensionStatus = when {
        item.isCurrent -> ExtensionStatus(
            status = "Current",
            displayText = "🔄 Currently Running",
            icon = "🔄",
            buttonText = "Reload"
        )
        isConfiguredForNextStartup(item.id) -> ExtensionStatus(
            status = "Next Startup", 
            displayText = "⏭️  Next Startup",
            icon = "⏭️",
            buttonText = "Will Activate Next Startup"
        )
        !item.isAvailable -> ExtensionStatus(
            status = "Uninstalled",
            displayText = "Uninstalled",
            icon = "",
            buttonText = "Switch"
        )
        item.resourceStatus.projectResourceExists || item.resourceStatus.vsixResourceExists -> ExtensionStatus(
            status = "Installed",
            displayText = "✅ Installed",
            icon = "✅",
            buttonText = "Switch"
        )
        else -> ExtensionStatus(
            status = "Built-in",
            displayText = "📦 Built-in",
            icon = "📦",
            buttonText = "Switch"
        )
    }
    
    /**
     * Check if an extension is configured for next startup
     */
    private fun isConfiguredForNextStartup(extensionId: String): Boolean {
        return extensionId == configManager.getCurrentExtensionId()
    }
    
    /**
     * Check if an extension is currently running
     */
    private fun isCurrentlyRunning(extensionId: String): Boolean {
        val currentProvider = extensionManager.getCurrentProvider()
        return currentProvider?.getExtensionId() == extensionId
    }
    
    private fun loadExtensions() {
        extensionListItems.clear()
        val current = extensionManager.getCurrentProvider()
        val providers = extensionManager.getAllProviders()
        
        providers.forEach { p ->
            val rs = checkResources(p)
            val item = ExtensionListItem(
                id = p.getExtensionId(),
                displayName = p.getDisplayName(),
                description = p.getDescription(),
                isAvailable = p.isAvailable(project),
                isCurrent = isCurrentlyRunning(p.getExtensionId()),
                resourceStatus = rs
            )
            extensionListItems.add(item)
        }
        
        extensionListItems.sortBy { it.displayName }
        extensionList.setListData(extensionListItems.toTypedArray())
        
        // Select current extension if exists
        val currentIndex = extensionListItems.indexOfFirst { it.isCurrent }
        if (currentIndex >= 0) {
            extensionList.selectedIndex = currentIndex
            selectedExtensionId = extensionListItems[currentIndex].id
            updateUI(extensionListItems[currentIndex])
        }
    }

    private fun updateUI(item: ExtensionListItem?) {
        if (item == null) {
            descriptionLabel.text = "Select an extension to view details"
            statusLabel.text = ""
            switchButton.isEnabled = false
            setAsDefaultCheckBox.isEnabled = false
            return
        }

        descriptionLabel.text = item.description
        
        // Enhanced status display
        val extensionStatus = getExtensionStatus(item)
        statusLabel.text = "Status: ${extensionStatus.displayText}"
        
        // Prevent switching if extension is already configured for next startup
        val canSwitch = item.isAvailable && 
                       !item.isCurrent && 
                       !isConfiguredForNextStartup(item.id) && 
                       !isSwitching
        switchButton.isEnabled = canSwitch
        
        // Update button text based on state
        switchButton.text = extensionStatus.buttonText
        
        setAsDefaultCheckBox.isEnabled = item.isAvailable && !item.isCurrent
    }

    // === Actions ==============================================================================
    private fun uploadVsixFile() {
        val selected = selectedExtensionId?.let { id -> extensionListItems.find { it.id == id } } ?: run {
            Messages.showWarningDialog("Please select an extension first.", "No Extension Selected")
            return
        }
        val success = VsixUploadDialog.show(project, selected.id, selected.displayName)
        if (success) {
            loadExtensions()
            Messages.showInfoMessage("VSIX file uploaded successfully: ${selected.displayName}", "Upload Complete")
        }
    }

    private fun performSwitch() {
        val target = selectedExtensionId ?: return
        val currentProvider = extensionManager.getCurrentProvider()
        val currentId = currentProvider?.getExtensionId()
        
        // Prevent switching if already configured for next startup
        if (isConfiguredForNextStartup(target)) {
            Messages.showInfoMessage(
                "Extension '$target' is already configured to activate on next startup.\n\n" +
                "No action needed.",
                "Already Configured"
            )
            return
        }
        
        if (currentId == target) {
            performReload(target)
            return
        }
        
        val confirm = Messages.showYesNoDialog(
            "Are you sure you want to switch from '$currentId' to '$target'?\n\n" +
            "⚠️  IMPORTANT: The extension will take effect on the next startup of IntelliJ IDEA.\n" +
            "The current session will continue using the existing extension.\n\n" +
            "Do you want to continue?",
            "Confirm Extension Switch",
            "Switch",
            "Cancel",
            Messages.getQuestionIcon()
        )
        
        if (confirm == Messages.YES) {
            if (setAsDefaultCheckBox.isSelected) {
                // TODO: Persist as project default
            }
            doSwitch(target)
        }
    }

    private fun performReload(extensionId: String) {
        // Prevent reload if extension is already configured for next startup
        if (isConfiguredForNextStartup(extensionId)) {
            Messages.showInfoMessage(
                "Extension '$extensionId' is already configured to activate on next startup.\n\n" +
                "No reload action needed.",
                "Already Configured"
            )
            return
        }
        
        isSwitching = true
        setSwitchingUI(true)
        
        extensionSwitcher.switchExtension(extensionId, true).whenComplete { success, err ->
            SwingUtilities.invokeLater {
                isSwitching = false
                setSwitchingUI(false)
                if (success) {
                    Messages.showInfoMessage(
                        "Extension configuration updated successfully: $extensionId\n\n" +
                        "Note: The extension will take effect on the next startup of IntelliJ IDEA.",
                        "Configuration Updated"
                    )
                    loadExtensions()
                } else {
                    val errorMsg = err?.message ?: "Unknown error occurred"
                    Messages.showErrorDialog("Failed to update extension configuration: $errorMsg", "Update Failed")
                }
            }
        }
    }

    private fun doSwitch(target: String) {
        isSwitching = true
        setSwitchingUI(true)
        
        extensionSwitcher.switchExtension(target, true).whenComplete { success, err ->
            SwingUtilities.invokeLater {
                isSwitching = false
                setSwitchingUI(false)
                if (success) {
                    Messages.showInfoMessage(
                        "Extension switch configuration saved successfully!\n\n" +
                        "✅ Extension: $target\n" +
                        "⚠️  The extension will take effect on the next startup of IntelliJ IDEA.\n" +
                        "🔄 Please restart IntelliJ IDEA to activate the new extension.",
                        "Extension Switch Complete"
                    )
                    // Refresh the UI to show the new configuration
                    loadExtensions()
                    // Don't close the dialog, let user see the updated state
                } else {
                    val errorMsg = err?.message ?: "Unknown error occurred"
                    Messages.showErrorDialog("Failed to save extension switch configuration: $errorMsg", "Configuration Save Failed")
                    loadExtensions()
                }
            }
        }
    }

    private fun setSwitchingUI(switching: Boolean) {
        switchButton.isEnabled = !switching
        installButton.isEnabled = !switching
        refreshButton.isEnabled = !switching
        
        if (switching) {
            switchButton.text = "Saving Configuration..."
        } else {
            val selected = selectedExtensionId?.let { id -> extensionListItems.find { it.id == id } }
            if (selected != null) {
                switchButton.text = getExtensionStatus(selected).buttonText
            } else {
                switchButton.text = "Switch"
            }
        }
    }

    override fun doCancelAction() {
        if (isSwitching) {
            val result = Messages.showYesNoDialog(
                "Configuration saving is in progress. Are you sure you want to cancel?",
                "Cancel Configuration Save",
                "Cancel Save",
                "Continue Waiting",
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

    // === Resource checks =====================================================================
    private fun checkResources(provider: ExtensionProvider): ResourceStatus {
        val cfg = provider.getConfiguration(project)
        val base = project.basePath
        
        var projExists = false
        var projPath: String? = null
        if (base != null) {
            listOf("$base/${cfg.getCodeDir()}", "$base/../${cfg.getCodeDir()}", "$base/../../${cfg.getCodeDir()}").forEach { p ->
                if (!projExists && File(p).exists()) {
                    projExists = true
                    projPath = p
                }
            }
        }
        
        var pluginExists = false
        var pluginPath: String? = null
        try {
            PluginResourceUtil.getResourcePath(PluginConstants.PLUGIN_ID, cfg.getCodeDir())?.let { path ->
                if (File(path).exists()) {
                    pluginExists = true
                    pluginPath = path
                }
            }
        } catch (_: Exception) {}
        
        val vsixMgr = VsixManager.getInstance()
        val extId = provider.getExtensionId()
        val vsixExists = vsixMgr.hasVsixInstallation(extId)
        val vsixPath = if (vsixExists) vsixMgr.getVsixInstallationPath(extId) else null
        
        return ResourceStatus(projExists, projPath, pluginExists, pluginPath, vsixExists, vsixPath)
    }
}