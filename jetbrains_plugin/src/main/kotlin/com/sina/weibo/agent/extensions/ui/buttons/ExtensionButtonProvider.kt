package com.sina.weibo.agent.extensions.ui.buttons

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project

/**
 * Extension button provider interface.
 * Each extension should implement this interface to provide its specific button configuration.
 */
interface ExtensionButtonProvider {

    /**
     * Get extension identifier.
     * @return Unique extension identifier
     */
    fun getExtensionId(): String

    /**
     * Get extension display name.
     * @return Human-readable extension name
     */
    fun getDisplayName(): String

    /**
     * Get extension description.
     * @return Extension description
     */
    fun getDescription(): String

    /**
     * Check if extension is available.
     * @param project Current project
     * @return true if extension is available, false otherwise
     */
    fun isAvailable(project: Project): Boolean

    /**
     * Get buttons for this extension.
     * @param project Current project
     * @return List of AnAction instances representing the buttons
     */
    fun getButtons(project: Project): List<AnAction>

    /**
     * Get button configuration for this extension.
     * @return ButtonConfiguration object defining button visibility
     */
    fun getButtonConfiguration(): ButtonConfiguration
}