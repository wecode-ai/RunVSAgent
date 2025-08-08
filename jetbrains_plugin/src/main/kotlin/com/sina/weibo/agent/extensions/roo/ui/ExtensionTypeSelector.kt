// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.extensions.roo.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.*
import com.intellij.util.ui.JBUI
import com.sina.weibo.agent.extensions.roo.ExtensionConfiguration
import com.sina.weibo.agent.extensions.roo.ExtensionType
import java.awt.Dimension
import javax.swing.DefaultListModel
import javax.swing.ListSelectionModel

/**
 * Extension type selector dialog for Roo Code
 * Allows users to select and switch between different extension types
 */
class ExtensionTypeSelector(private val project: Project) : DialogWrapper(project) {
    
    private val extensionConfig = ExtensionConfiguration.getInstance(project)
    private val listModel = DefaultListModel<ExtensionType>()
    private val extensionList = JBList(listModel)
    
    init {
        title = "Select Extension Type"
        init()
        loadExtensionTypes()
    }
    
    private fun loadExtensionTypes() {
        listModel.clear()
        ExtensionType.getAllTypes().forEach { listModel.addElement(it) }
        
        // Select current extension type
        val currentType = extensionConfig.getCurrentExtensionType()
        extensionList.setSelectedValue(currentType, true)
    }
    
    override fun createCenterPanel() = panel {
        row {
            label("Select an extension type to use:")
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
                val selectedType = extensionList.selectedValue
                if (selectedType != null) {
                    descriptionLabel.setText(selectedType.description)
                }
            }
            cell(descriptionLabel).resizableColumn()
        }
    }
    
    override fun doOKAction() {
        val selectedType = extensionList.selectedValue
        if (selectedType != null) {
            extensionConfig.setCurrentExtensionType(selectedType)
            super.doOKAction()
        }
    }
    
    override fun doValidate(): ValidationInfo? {
        val selectedType = extensionList.selectedValue
        if (selectedType == null) {
            return ValidationInfo("Please select an extension type")
        }
        return null
    }
    
    companion object {
        /**
         * Show extension type selector dialog
         */
        fun show(project: Project): Boolean {
            val dialog = ExtensionTypeSelector(project)
            return dialog.showAndGet()
        }
    }
} 