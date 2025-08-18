// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.extensions.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.*
import com.sina.weibo.agent.extensions.core.ExtensionManager
import com.sina.weibo.agent.extensions.config.ExtensionProvider
import java.awt.Dimension
import javax.swing.DefaultListModel

/**
 * Extension selector dialog
 * Allows users to select and switch between different extension providers
 */
class ExtensionSelector(private val project: Project) : DialogWrapper(project) {
    
    private val extensionManager = ExtensionManager.getInstance(project)
    private val listModel = DefaultListModel<ExtensionProvider>()
    private val extensionList = JBList(listModel)
    
    init {
        title = "Select Extension"
        init()
        loadExtensions()
    }
    
    private fun loadExtensions() {
        listModel.clear()
        val availableProviders = extensionManager.getAvailableProviders()
        availableProviders.forEach { listModel.addElement(it) }
        
        // Select current extension provider
        val currentProvider = extensionManager.getCurrentProvider()
        if (currentProvider != null) {
            extensionList.setSelectedValue(currentProvider, true)
        }
    }
    
    override fun createCenterPanel() = panel {
        row {
            label("Select an extension to use:")
        }
        
        row {
            cell(JBScrollPane(extensionList).apply {
                preferredSize = Dimension(400, 200)
            }).resizableColumn()
        }
        
        row {
            label("Description:").bold()
        }
        
        row {
            val descriptionLabel = JBLabel("")
            extensionList.addListSelectionListener {
                val selectedProvider = extensionList.selectedValue
                if (selectedProvider != null) {
                    descriptionLabel.setText(selectedProvider.getDescription())
                }
            }
            cell(descriptionLabel).resizableColumn()
        }
    }
    
    override fun doOKAction() {
        val selectedProvider = extensionList.selectedValue
        if (selectedProvider != null) {
            extensionManager.setCurrentProvider(selectedProvider.getExtensionId())
            super.doOKAction()
        }
    }
    
    override fun doValidate(): ValidationInfo? {
        val selectedProvider = extensionList.selectedValue
        if (selectedProvider == null) {
            return ValidationInfo("Please select an extension")
        }
        return null
    }
    
    companion object {
        /**
         * Show extension selector dialog
         */
        fun show(project: Project): Boolean {
            val dialog = ExtensionSelector(project)
            return dialog.showAndGet()
        }
    }
} 