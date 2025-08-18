package com.sina.weibo.agent.extensions.common

import com.intellij.util.messages.Topic

/**
 * Extension change listener interface.
 * Components can implement this interface to be notified when the current extension changes.
 */
interface ExtensionChangeListener {

    /**
     * Called when the current extension changes.
     *
     * @param newExtensionId The ID of the new extension
     */
    fun onExtensionChanged(newExtensionId: String)

    companion object {
        /**
         * Topic for extension change events.
         * Components can subscribe to this topic to receive extension change notifications.
         */
        val EXTENSION_CHANGE_TOPIC = Topic.create("Extension Change", ExtensionChangeListener::class.java)
    }
}