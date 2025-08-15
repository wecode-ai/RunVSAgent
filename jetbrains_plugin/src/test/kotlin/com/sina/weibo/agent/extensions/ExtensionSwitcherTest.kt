// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.extensions

import org.junit.Test
import org.junit.Assert.*

/**
 * Simple test for ExtensionSwitcher functionality
 */
class ExtensionSwitcherTest {
    
    @Test
    fun testExtensionChangeListenerTopic() {
        // Test that the topic is properly defined
        assertNotNull("Extension change topic should not be null", ExtensionChangeListener.EXTENSION_CHANGE_TOPIC)
    }
    
    @Test
    fun testProjectConfigDefault() {
        // Test default configuration
        val defaultConfig = ProjectConfig.getDefault()
        assertEquals("Default extension type should be cline", "cline", defaultConfig.extensionType)
        assertFalse("Default auto-switch should be false", defaultConfig.autoSwitch)
        assertEquals("Default debug mode should be none", "none", defaultConfig.debugMode)
        assertEquals("Default log level should be info", "info", defaultConfig.logLevel)
        assertTrue("Default extension settings should be empty", defaultConfig.extensionSettings.isEmpty())
    }
}
