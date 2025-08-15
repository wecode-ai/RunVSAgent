// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.extensions

import com.intellij.util.messages.Topic

/**
 * Extension change listener interface
 * Components can implement this to receive extension change notifications
 */
interface ExtensionChangeListener {
    
    /**
     * Called when extension is changed
     * @param newExtensionId New extension ID
     */
    fun onExtensionChanged(newExtensionId: String)
    
    /**
     * Called before extension change starts
     * @param oldExtensionId Current extension ID
     * @param newExtensionId Target extension ID
     */
    fun onExtensionChangeStarting(oldExtensionId: String, newExtensionId: String)
    
    /**
     * Called when extension change is completed
     * @param newExtensionId New extension ID
     * @param success Whether the change was successful
     */
    fun onExtensionChangeCompleted(newExtensionId: String, success: Boolean)
    
    companion object {
        val EXTENSION_CHANGE_TOPIC = Topic.create("Extension Change", ExtensionChangeListener::class.java)
    }
}
