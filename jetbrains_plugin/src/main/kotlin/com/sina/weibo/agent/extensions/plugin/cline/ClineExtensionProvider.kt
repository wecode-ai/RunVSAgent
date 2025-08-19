// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.extensions.plugin.cline

import com.intellij.openapi.project.Project
import com.sina.weibo.agent.extensions.config.ExtensionConfiguration
import com.sina.weibo.agent.extensions.core.ExtensionManagerFactory
import com.sina.weibo.agent.extensions.core.VsixManager
import com.sina.weibo.agent.extensions.config.ExtensionProvider
import com.sina.weibo.agent.extensions.common.ExtensionType
import com.sina.weibo.agent.extensions.config.ExtensionMetadata
import com.sina.weibo.agent.util.PluginConstants
import com.sina.weibo.agent.util.PluginResourceUtil
import java.io.File

/**
 * Cline AI extension provider implementation
 */
class ClineExtensionProvider : ExtensionProvider {
    
    override fun getExtensionId(): String = "cline"
    
    override fun getDisplayName(): String = "Cline AI"
    
    override fun getDescription(): String = "AI-powered coding assistant with advanced features"
    
    override fun initialize(project: Project) {
        // Initialize cline extension configuration
        val extensionConfig = ExtensionConfiguration.getInstance(project)
        extensionConfig.initialize()
        
        // Initialize extension manager factory if needed
        try {
            val extensionManagerFactory = ExtensionManagerFactory.getInstance(project)
            extensionManagerFactory.initialize()
        } catch (e: Exception) {
            // If ExtensionManagerFactory is not available, continue without it
            // This allows cline to work independently
        }
    }
    
    override fun isAvailable(project: Project): Boolean {
        // Always return true since we now support uploading VSIX files after startup
        // The actual resource availability will be checked when needed
        return true
    }
    
    override fun getConfiguration(project: Project): ExtensionMetadata {
        val extensionConfig = ExtensionConfiguration.getInstance(project)
        val config = extensionConfig.getConfig(ExtensionType.CLINE)
        
        return object : ExtensionMetadata {
            override fun getCodeDir(): String = config.codeDir
            override fun getPublisher(): String = config.publisher
            override fun getVersion(): String = config.version
            override fun getMainFile(): String = config.mainFile
            override fun getActivationEvents(): List<String> = config.activationEvents
            override fun getEngines(): Map<String, String> = config.engines
            override fun getCapabilities(): Map<String, Any> = config.capabilities
            override fun getExtensionDependencies(): List<String> = config.extensionDependencies
        }
    }
    
    override fun dispose() {
        // Cleanup resources if needed
    }
}
