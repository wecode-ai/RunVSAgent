// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.extensions.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBCheckBox
import javax.swing.JButton
import com.sina.weibo.agent.extensions.core.ExtensionManager
import com.sina.weibo.agent.extensions.core.ExtensionSwitcher
import com.sina.weibo.agent.extensions.core.ExtensionConfigurationManager
import com.sina.weibo.agent.extensions.core.VsixManager
import com.sina.weibo.agent.extensions.config.ExtensionProvider
import com.sina.weibo.agent.util.PluginResourceUtil
import com.sina.weibo.agent.util.PluginConstants
import java.awt.BorderLayout
import java.awt.Dimension
import java.io.File
import javax.swing.*
import javax.swing.event.ListSelectionListener

/**
 * Extension switcher dialog
 * Provides UI for switching between different extension providers
 */
class ExtensionSwitcherDialog(private val project: Project) : DialogWrapper(project) {
    
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
        val panel = JPanel(BorderLayout())
        panel.preferredSize = Dimension(500, 400)
        
        // Extension list
        extensionList = JBList<ExtensionItem>()
        extensionList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        extensionList.addListSelectionListener(createListSelectionListener())
        
        // Set custom cell renderer to show resource status
        extensionList.setCellRenderer { list, value, index, isSelected, cellHasFocus ->
            val label = JBLabel()
            if (value is ExtensionItem) {
                val resourceIcon = when {
                    value.resourceStatus.projectResourceExists -> "🟢"
                    value.resourceStatus.vsixResourceExists -> "🔵"
                    value.resourceStatus.pluginResourceExists -> "🟡"
                    else -> "🔴"
                }
                
                val displayText = "${resourceIcon} ${value.displayName}"
                if (value.isCurrent) {
                    label.text = "<html><b>$displayText</b> <i>(Current)</i></html>"
                } else {
                    label.text = displayText
                }
                
                // Set tooltip with detailed resource information
                val tooltipText = buildString {
                    append("Extension: ${value.displayName}\n")
                    append("Status: ${value.resourceStatus.statusText}\n")
                    if (value.resourceStatus.projectResourceExists) {
                        append("Local Path: ${value.resourceStatus.projectResourcePath}\n")
                    }
                    if (value.resourceStatus.vsixResourceExists) {
                        append("VSIX Path: ${value.resourceStatus.vsixResourcePath}\n")
                    }
                    if (value.resourceStatus.pluginResourceExists) {
                        append("Plugin Path: ${value.resourceStatus.pluginResourcePath}\n")
                    }
                    if (!value.resourceStatus.projectResourceExists && !value.resourceStatus.vsixResourceExists && !value.resourceStatus.pluginResourceExists) {
                        append("⚠️ Warning: No resource files found!")
                    }
                }
                label.toolTipText = tooltipText
                
                // Set background color based on selection
                if (isSelected) {
                    label.background = list.selectionBackground
                    label.foreground = list.selectionForeground
                } else {
                    label.background = list.background
                    label.foreground = list.foreground
                }
                label.isOpaque = true
            }
            label
        }
        
        val listScrollPane = JScrollPane(extensionList)
        listScrollPane.preferredSize = Dimension(300, 200)
        
        // Right panel for details
        val rightPanel = createRightPanel()
        
        // Main layout
        panel.add(createTopPanel(), BorderLayout.NORTH)
        panel.add(listScrollPane, BorderLayout.WEST)
        panel.add(rightPanel, BorderLayout.CENTER)
        panel.add(createBottomPanel(), BorderLayout.SOUTH)
        
        return panel
    }
    
    /**
     * Create top panel
     */
    private fun createTopPanel(): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        
        // Main title
        panel.add(JBLabel("Select Extension Provider:"))
        
        // Status legend
        val legendText = "<html><b>Resource Status Legend:</b><br>" +
                "🟢 Local Resources | 🔵 VSIX Installation | 🟡 Built-in Resources | 🔴 No Resources</html>"
        val legendLabel = JBLabel(legendText)
        legendLabel.preferredSize = Dimension(400, 50)
        panel.add(legendLabel)
        
        return panel
    }
    
    /**
     * Create right panel
     */
    private fun createRightPanel(): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        
        // Description
        descriptionLabel = JBLabel("Select an extension to view details")
        descriptionLabel.preferredSize = Dimension(200, 60)
        panel.add(descriptionLabel)
        
        // Status
        statusLabel = JBLabel("")
        statusLabel.preferredSize = Dimension(200, 40)
        panel.add(statusLabel)
        
        // Resource Status
        resourceStatusLabel = JBLabel("")
        resourceStatusLabel.preferredSize = Dimension(200, 60)
        panel.add(resourceStatusLabel)
        
        // Upload VSIX Button
        uploadVsixButton = JButton("Upload VSIX File")
        uploadVsixButton.preferredSize = Dimension(150, 30)
        uploadVsixButton.addActionListener { uploadVsixFile() }
        panel.add(uploadVsixButton)
        
        // Auto-switch option (not implemented yet)
        autoSwitchCheckBox = JBCheckBox("Remember this choice for future projects")
        autoSwitchCheckBox.isSelected = false // Default to false until implemented
        autoSwitchCheckBox.isEnabled = false // Disable until implemented
        panel.add(autoSwitchCheckBox)
        
        panel.add(Box.createVerticalGlue())
        
        return panel
    }
    
    /**
     * Create bottom panel
     */
    private fun createBottomPanel(): JPanel {
        val panel = JPanel()
        
        // Switch button
        switchButton = JButton("Switch Extension")
        switchButton.isEnabled = false
        switchButton.addActionListener { performSwitch() }
        
        // Cancel button
        cancelButton = JButton("Cancel")
        cancelButton.addActionListener { doCancelAction() }
        
        panel.add(switchButton)
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
        descriptionLabel.text = "<html><b>${item.displayName}</b><br><br>${item.description}</html>"
        
        val statusText = when {
            item.isCurrent -> "Currently active"
            !item.isAvailable -> "Not available"
            else -> "Available"
        }
        statusLabel.text = statusText
        
        // Update resource status display
        updateResourceStatusDisplay(item.resourceStatus)
        
        // Update upload button state
        updateUploadButtonState(item.resourceStatus)
    }
    
    /**
     * Update upload button state based on resource status
     */
    private fun updateUploadButtonState(resourceStatus: ResourceStatus) {
        val needsUpload = !resourceStatus.projectResourceExists && 
                         !resourceStatus.vsixResourceExists && 
                         !resourceStatus.pluginResourceExists
        
        // Always show the upload button, but enable it only when needed
        uploadVsixButton.isVisible = true
        
        if (needsUpload) {
            uploadVsixButton.text = "Upload VSIX File"
            uploadVsixButton.isEnabled = true
            uploadVsixButton.toolTipText = "Upload VSIX file to install extension resources"
        } else {
            uploadVsixButton.text = "Resources Available"
            uploadVsixButton.isEnabled = false
            uploadVsixButton.toolTipText = "Extension resources are already available"
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
        } catch (e: Exception) {
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
        val statusHtml = buildString {
            append("<html><b>Resource Status:</b><br>")
            append("${resourceStatus.statusIcon} ${resourceStatus.statusText}<br><br>")
            
            if (resourceStatus.projectResourceExists) {
                append("<b>Local Path:</b><br>")
                append("<code>${resourceStatus.projectResourcePath}</code><br><br>")
            }
            
            if (resourceStatus.vsixResourceExists) {
                append("<b>VSIX Installation:</b><br>")
                append("<code>${resourceStatus.vsixResourcePath}</code><br><br>")
            }
            
            if (resourceStatus.pluginResourceExists) {
                append("<b>Plugin Path:</b><br>")
                append("<code>${resourceStatus.pluginResourcePath}</code><br>")
            }
            
            if (!resourceStatus.projectResourceExists && !resourceStatus.vsixResourceExists && !resourceStatus.pluginResourceExists) {
                append("<font color='red'>⚠️ Warning: No resource files found!</font><br>")
                append("This extension may not function properly.<br>")
                append("You can upload a VSIX file to install the extension.")
            }
        }
        
        resourceStatusLabel.text = statusHtml
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
            val hasResources = selectedItem.resourceStatus.projectResourceExists || 
                              selectedItem.resourceStatus.vsixResourceExists || 
                              selectedItem.resourceStatus.pluginResourceExists
            
            if (hasResources) {
                switchButton.text = "Switch Extension"
                switchButton.toolTipText = "Switch to ${selectedItem.displayName}"
            } else {
                switchButton.text = "Switch & Upload VSIX"
                switchButton.toolTipText = "Switch to ${selectedItem.displayName} and upload VSIX file for resources"
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
