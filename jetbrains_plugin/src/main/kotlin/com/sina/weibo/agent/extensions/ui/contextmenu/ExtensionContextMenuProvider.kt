package com.sina.weibo.agent.extensions.ui.contextmenu

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project

/**
 * Extension context menu provider interface.
 * Each extension should implement this interface to provide its specific context menu actions.
 * This allows different extensions to have different right-click context menus.
 */
interface ExtensionContextMenuProvider {

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
     * Get context menu actions for this extension.
     * @param project Current project
     * @return List of AnAction instances representing the context menu actions
     */
    fun getContextMenuActions(project: Project): List<AnAction>

    /**
     * Get context menu configuration for this extension.
     * @return ContextMenuConfiguration object defining action visibility
     */
    fun getContextMenuConfiguration(): ContextMenuConfiguration
}
