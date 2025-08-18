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
import java.awt.BorderLayout
import java.awt.Dimension
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
        val isCurrent: Boolean
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
        panel.add(JBLabel("Select Extension Provider:"))
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
            val item = ExtensionItem(
                id = provider.getExtensionId(),
                displayName = provider.getDisplayName(),
                description = provider.getDescription(),
                isAvailable = provider.isAvailable(project),
                isCurrent = provider == currentProvider
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
    }
    
    /**
     * Update switch button state
     */
    private fun updateSwitchButton() {
        val selectedItem = selectedExtensionId?.let { id ->
            extensionItems.find { it.id == id }
        }
        
        switchButton.isEnabled = selectedItem != null && 
                                selectedItem.isAvailable && 
                                !selectedItem.isCurrent &&
                                !extensionSwitcher.isSwitching()
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
