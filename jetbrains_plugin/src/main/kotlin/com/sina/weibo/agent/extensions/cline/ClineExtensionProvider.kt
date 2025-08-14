// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.extensions.cline

import com.intellij.openapi.project.Project
import com.sina.weibo.agent.extensions.ExtensionProvider
import com.sina.weibo.agent.extensions.ExtensionConfiguration as BaseExtensionConfiguration

/**
 * Cline AI extension provider implementation
 */
class ClineExtensionProvider : ExtensionProvider {
    
    override fun getExtensionId(): String = "cline"
    
    override fun getDisplayName(): String = "Cline AI"
    
    override fun getDescription(): String = "AI-powered coding assistant with advanced features"
    
    override fun initialize(project: Project) {
        // Initialize cline extension configuration
        val extensionConfig = com.sina.weibo.agent.extensions.roo.ExtensionConfiguration.getInstance(project)
        extensionConfig.initialize()
        
        // Initialize extension manager factory if needed
        try {
            val extensionManagerFactory = com.sina.weibo.agent.extensions.roo.ExtensionManagerFactory.getInstance(project)
            extensionManagerFactory.initialize()
        } catch (e: Exception) {
            // If ExtensionManagerFactory is not available, continue without it
            // This allows cline to work independently
        }
    }
    
    override fun isAvailable(project: Project): Boolean {
        // Check if cline extension files exist
        val extensionConfig = com.sina.weibo.agent.extensions.roo.ExtensionConfiguration.getInstance(project)
        val config = extensionConfig.getConfig(com.sina.weibo.agent.extensions.roo.ExtensionType.CLINE)
        
        // First check project paths
        val projectPath = project.basePath
        if (projectPath != null) {
            val possiblePaths = listOf(
                "$projectPath/${config.codeDir}",
                "$projectPath/../${config.codeDir}",
                "$projectPath/../../${config.codeDir}"
            )
            
            if (possiblePaths.any { java.io.File(it).exists() }) {
                return true
            }
        }
        
        // Then check plugin resources (for built-in extensions)
        try {
            val pluginResourcePath = com.sina.weibo.agent.util.PluginResourceUtil.getResourcePath(
                com.sina.weibo.agent.util.PluginConstants.PLUGIN_ID, 
                config.codeDir
            )
            if (pluginResourcePath != null && java.io.File(pluginResourcePath).exists()) {
                return true
            }
        } catch (e: Exception) {
            // Ignore exceptions when checking plugin resources
        }
        
        // For development/testing, always return true if we can't find the files
        // This allows the extension to work even without the actual extension files
        return true
    }
    
    override fun getConfiguration(project: Project): BaseExtensionConfiguration {
        val extensionConfig = com.sina.weibo.agent.extensions.roo.ExtensionConfiguration.getInstance(project)
        val config = extensionConfig.getConfig(com.sina.weibo.agent.extensions.roo.ExtensionType.CLINE)
        
        return object : BaseExtensionConfiguration {
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
