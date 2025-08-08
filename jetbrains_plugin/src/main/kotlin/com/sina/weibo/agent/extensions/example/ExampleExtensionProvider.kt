// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.extensions.example

import com.intellij.openapi.project.Project
import com.sina.weibo.agent.extensions.ExtensionProvider
import com.sina.weibo.agent.extensions.ExtensionConfiguration as BaseExtensionConfiguration

/**
 * Example extension provider
 * This is a template for creating new extension providers
 */
class ExampleExtensionProvider : ExtensionProvider {
    
    override fun getExtensionId(): String = "example-extension"
    
    override fun getDisplayName(): String = "Example Extension"
    
    override fun getDescription(): String = "This is an example extension for demonstration purposes"
    
    override fun initialize(project: Project) {
        // Initialize your extension here
        // This is called when the extension is selected
    }
    
    override fun isAvailable(project: Project): Boolean {
        // Check if your extension is available
        // For example, check if required files exist
        val projectPath = project.basePath ?: return false
        val extensionPath = "$projectPath/example-extension"
        return java.io.File(extensionPath).exists()
    }
    
    override fun getConfiguration(project: Project): BaseExtensionConfiguration {
        return object : BaseExtensionConfiguration {
            override fun getCodeDir(): String = "example-extension"
            override fun getPublisher(): String = "Example Publisher"
            override fun getVersion(): String = "1.0.0"
            override fun getMainFile(): String = "./dist/extension.js"
            override fun getActivationEvents(): List<String> = listOf("onStartupFinished")
            override fun getEngines(): Map<String, String> = mapOf("vscode" to "^1.0.0")
            override fun getCapabilities(): Map<String, Any> = emptyMap()
            override fun getExtensionDependencies(): List<String> = emptyList()
        }
    }
    
    override fun dispose() {
        // Cleanup resources when extension is disposed
    }
} 