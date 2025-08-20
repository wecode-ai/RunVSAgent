// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.extensions.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooser
import com.sina.weibo.agent.extensions.core.VsixManager
import java.io.File

/**
 * VSIX file upload dialog for extensions
 * Allows users to upload VSIX files when extension resources are not found
 */
class VsixUploadDialog(
    private val project: Project,
    private val extensionId: String,
    private val extensionName: String
) : DialogWrapper(project) {
    
    private var selectedVsixFile: File? = null
    
    init {
        title = "Upload VSIX for $extensionName"
        init()
        // Set dialog size
        setSize(500, 150)
    }
    
    override fun createCenterPanel() = panel {
        row {
            label("Extension: $extensionName")
        }
        
        row {
            label("Select VSIX File:")
        }
        
        row {
            val fileField = JBTextField().apply {
                isEditable = false
                columns = 50
            }
            
            cell(fileField).resizableColumn()
            
            button("Browse") {
                selectVsixFile(fileField)
            }
        }
    }
    
    private fun selectVsixFile(fileField: JBTextField) {
        val descriptor = FileChooserDescriptor(true, false, false, false, false, false)
            .withFileFilter { file -> file.extension?.lowercase() == "vsix" }
            .withTitle("Select VSIX File")
        
        FileChooser.chooseFile(descriptor, project, null) { file ->
            selectedVsixFile = file.toNioPath().toFile()
            fileField.text = file.path
        }
    }
    
    override fun doOKAction() {
        if (selectedVsixFile == null) {
            Messages.showErrorDialog(
                "Please select a VSIX file to upload.",
                "No File Selected"
            )
            return
        }
        
        if (!selectedVsixFile!!.exists()) {
            Messages.showErrorDialog(
                "Selected file does not exist.",
                "File Not Found"
            )
            return
        }
        
        try {
            val vsixManager = VsixManager.getInstance()
            val success = vsixManager.installVsix(selectedVsixFile!!, extensionId)
            if (success) {
                Messages.showInfoMessage(
                    "VSIX file uploaded successfully!",
                    "Upload Complete"
                )
                super.doOKAction()
            } else {
                Messages.showErrorDialog(
                    "Failed to extract VSIX file. Please check the file format and try again.",
                    "Extraction Failed"
                )
            }
        } catch (e: Exception) {
            Messages.showErrorDialog(
                "Error during upload: ${e.message}",
                "Upload Error"
            )
        }
    }

    companion object {
        /**
         * Show VSIX upload dialog
         */
        fun show(project: Project, extensionId: String, extensionName: String): Boolean {
            val dialog = VsixUploadDialog(project, extensionId, extensionName)
            return dialog.showAndGet()
        }
    }
}
