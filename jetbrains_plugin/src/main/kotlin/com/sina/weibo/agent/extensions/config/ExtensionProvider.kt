// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.extensions.config

import com.intellij.openapi.project.Project

/**
 * Extension provider interface
 * All extension implementations should implement this interface
 */
interface ExtensionProvider {
    /**
     * Get extension identifier
     * @return Unique extension identifier
     */
    fun getExtensionId(): String
    
    /**
     * Get extension display name
     * @return Human-readable extension name
     */
    fun getDisplayName(): String
    
    /**
     * Get extension description
     * @return Extension description
     */
    fun getDescription(): String
    
    /**
     * Initialize extension
     * @param project Current project
     */
    fun initialize(project: Project)
    
    /**
     * Check if extension is available
     * @param project Current project
     * @return true if extension is available, false otherwise
     */
    fun isAvailable(project: Project): Boolean
    
    /**
     * Get extension configuration
     * @param project Current project
     * @return Extension configuration
     */
    fun getConfiguration(project: Project): ExtensionMetadata
    
    /**
     * Dispose extension resources
     */
    fun dispose()
}

/**
 * Extension configuration interface
 */
interface ExtensionMetadata {
    /**
     * Get extension directory name
     * @return Directory name where extension files are located
     */
    fun getCodeDir(): String
    
    /**
     * Get extension publisher
     * @return Extension publisher name
     */
    fun getPublisher(): String
    
    /**
     * Get extension version
     * @return Extension version
     */
    fun getVersion(): String
    
    /**
     * Get main entry file
     * @return Main JavaScript file path
     */
    fun getMainFile(): String
    
    /**
     * Get activation events
     * @return List of activation events
     */
    fun getActivationEvents(): List<String>
    
    /**
     * Get engine requirements
     * @return Map of engine requirements
     */
    fun getEngines(): Map<String, String>
    
    /**
     * Get extension capabilities
     * @return Map of extension capabilities
     */
    fun getCapabilities(): Map<String, Any>
    
    /**
     * Get extension dependencies
     * @return List of extension dependencies
     */
    fun getExtensionDependencies(): List<String>
} 