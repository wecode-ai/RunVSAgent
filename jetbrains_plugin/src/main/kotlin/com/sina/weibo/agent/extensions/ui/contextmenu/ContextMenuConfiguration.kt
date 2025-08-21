package com.sina.weibo.agent.extensions.ui.contextmenu

/**
 * Context menu configuration interface.
 * Defines which context menu actions should be visible for a specific extension.
 */
interface ContextMenuConfiguration {
    
    /**
     * Check if a specific context menu action should be visible.
     * @param actionType The type of context menu action
     * @return true if the action should be visible, false otherwise
     */
    fun isActionVisible(actionType: ContextMenuActionType): Boolean
    
    /**
     * Get all visible context menu actions.
     * @return List of visible action types
     */
    fun getVisibleActions(): List<ContextMenuActionType>
}

/**
 * Context menu action types that can be configured.
 * These represent the different types of right-click context menu actions.
 */
enum class ContextMenuActionType {
    EXPLAIN_CODE,
    FIX_CODE,
    FIX_LOGIC,
    IMPROVE_CODE,
    ADD_TO_CONTEXT,
    NEW_TASK
}
