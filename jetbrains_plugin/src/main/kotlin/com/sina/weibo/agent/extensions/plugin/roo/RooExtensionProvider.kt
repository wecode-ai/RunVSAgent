// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.extensions.plugin.roo

import com.intellij.openapi.project.Project
import com.sina.weibo.agent.extensions.common.ExtensionType
import com.sina.weibo.agent.extensions.config.ExtensionConfiguration
import com.sina.weibo.agent.extensions.core.ExtensionManagerFactory
import com.sina.weibo.agent.extensions.config.ExtensionProvider
import com.sina.weibo.agent.extensions.config.ExtensionMetadata
import com.sina.weibo.agent.util.PluginConstants
import com.sina.weibo.agent.util.PluginResourceUtil
import java.io.File

/**
 * Roo Code extension provider implementation
 */
class RooExtensionProvider : ExtensionProvider {
    
    override fun getExtensionId(): String = "roo-code"
    
    override fun getDisplayName(): String = "Roo Code"
    
    override fun getDescription(): String = "AI-powered code assistant"
    
    override fun initialize(project: Project) {
        // Initialize roo extension configuration
        val extensionConfig = ExtensionConfiguration.getInstance(project)
        extensionConfig.initialize()
        
        // Initialize extension manager factory
        val extensionManagerFactory = ExtensionManagerFactory.getInstance(project)
        extensionManagerFactory.initialize()
    }
    
    override fun isAvailable(project: Project): Boolean {
        // Check if roo-code extension files exist
        val extensionConfig = ExtensionConfiguration.getInstance(project)
        val config = extensionConfig.getConfig(ExtensionType.ROO_CODE)
        
        // First check project paths
        val homeDir = System.getProperty("user.home")
        val possiblePaths = listOf(
            "$homeDir/.run-vs-agent/plugins/${config.codeDir}"
        )

        if (possiblePaths.any { File(it).exists() }) {
            return true
        }
        
        // Then check plugin resources (for built-in extensions)
        try {
            val pluginResourcePath = PluginResourceUtil.getResourcePath(
                PluginConstants.PLUGIN_ID,
                config.codeDir
            )
            if (pluginResourcePath != null && File(pluginResourcePath).exists()) {
                return true
            }
        } catch (e: Exception) {
            // Ignore exceptions when checking plugin resources
        }
        
        // For development/testing, always return true if we can't find the files
        // This allows the extension to work even without the actual extension files
        return false
    }
    
    override fun getConfiguration(project: Project): ExtensionMetadata {
        val extensionConfig = ExtensionConfiguration.getInstance(project)
        val config = extensionConfig.getConfig(ExtensionType.ROO_CODE);

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