// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.extensions.plugin.kilo

import com.intellij.openapi.project.Project
import com.sina.weibo.agent.extensions.config.ExtensionConfiguration
import com.sina.weibo.agent.extensions.core.ExtensionManagerFactory
import com.sina.weibo.agent.extensions.core.VsixManager
import com.sina.weibo.agent.extensions.config.ExtensionProvider
import com.sina.weibo.agent.extensions.common.ExtensionType
import com.sina.weibo.agent.extensions.config.ExtensionMetadata
import com.sina.weibo.agent.extensions.core.VsixManager.Companion.getBaseDirectory
import com.sina.weibo.agent.util.PluginConstants
import com.sina.weibo.agent.util.PluginResourceUtil
import java.io.File

/**
 * Kilo Code extension provider implementation
 */
class KiloCodeExtensionProvider : ExtensionProvider {
    
    override fun getExtensionId(): String = "kilo-code"
    
    override fun getDisplayName(): String = "Kilo Code"
    
    override fun getDescription(): String = "AI-powered code assistant with advanced capabilities"
    
    override fun initialize(project: Project) {
        // Initialize kilo-code extension configuration
        val extensionConfig = ExtensionConfiguration.getInstance(project)
        extensionConfig.initialize()
        
        // Initialize extension manager factory if needed
        try {
            val extensionManagerFactory = ExtensionManagerFactory.getInstance(project)
            extensionManagerFactory.initialize()
        } catch (e: Exception) {
            // If ExtensionManagerFactory is not available, continue without it
            // This allows kilo-code to work independently
        }
    }

    override fun isAvailable(project: Project): Boolean {
        // Check if kilo-code extension files exist
        val extensionConfig = ExtensionConfiguration.getInstance(project)
        val config = extensionConfig.getConfig(ExtensionType.KILO_CODE)

        // First check project paths
        val possiblePaths = listOf(
            "${getBaseDirectory()}/${config.codeDir}"
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
            // Plugin resource not available, continue checking other paths
        }
        
        return false
    }
    
    override fun getConfiguration(project: Project): ExtensionMetadata {
        val extensionConfig = ExtensionConfiguration.getInstance(project)
        val config = extensionConfig.getConfig(ExtensionType.KILO_CODE)
        
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