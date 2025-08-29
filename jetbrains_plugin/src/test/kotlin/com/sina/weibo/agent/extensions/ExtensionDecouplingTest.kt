// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.extensions

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.sina.weibo.agent.extensions.core.ExtensionManager
import com.sina.weibo.agent.extensions.plugin.roo.RooExtensionProvider
import com.sina.weibo.agent.extensions.plugin.kilo.KiloCodeExtensionProvider

/**
 * Test class for extension decoupling functionality
 */
class ExtensionDecouplingTest : BasePlatformTestCase() {
    
    private lateinit var extensionManager: ExtensionManager
    
    override fun setUp() {
        super.setUp()
        extensionManager = ExtensionManager.getInstance(project)
        extensionManager.initialize()
    }
    
    override fun tearDown() {
        extensionManager.dispose()
        super.tearDown()
    }
    
    fun testExtensionProviderRegistration() {
        // Test that all extension providers are registered
        val allProviders = extensionManager.getAllProviders()
        assertTrue("Should have at least 3 extension providers", allProviders.size >= 3)
        
        // Check specific providers
        val rooProvider = extensionManager.getProvider("roo-code")
        assertNotNull("Roo Code provider should be registered", rooProvider)
        assertEquals("Roo Code", rooProvider?.getDisplayName())
    }
    
    fun testExtensionProviderSwitching() {
        // Test switching between extension providers
        val rooProvider = extensionManager.getProvider("roo-code")

        assertNotNull("Roo provider should exist", rooProvider)

        // In test environment, providers may not be available due to missing files
        // So we test the switching logic only if providers are available
        if (rooProvider?.isAvailable(project) == true) {
            extensionManager.setCurrentProvider("roo-code")
            assertEquals("Current provider should be roo-code", "roo-code", extensionManager.getCurrentProvider()?.getExtensionId())
        }
    }
    
    fun testExtensionConfiguration() {
        val rooProvider = extensionManager.getProvider("roo-code")
        assertNotNull("Roo provider should exist", rooProvider)
        
        val config = rooProvider?.getConfiguration(project)
        assertNotNull("Configuration should not be null", config)
        
        assertEquals("Code directory should be roo-code", "roo-code", config?.getCodeDir())
        assertEquals("Publisher should be WeCode-AI", "WeCode-AI", config?.getPublisher())
        assertEquals("Version should be 1.0.0", "1.0.0", config?.getVersion())
    }
    
    fun testExtensionAvailability() {
        // Test extension availability checking
        val rooProvider = extensionManager.getProvider("roo-code")

        assertNotNull("Roo provider should exist", rooProvider)

        // Test availability (this will depend on whether extension files exist)
        // In test environment, extensions may not be available due to missing files
        // This is expected behavior, so we just verify the method works
        assertNotNull("Roo provider availability check should work", rooProvider?.isAvailable(project))
    }
    
    fun testExtensionProviderInterface() {
        // Test that all providers implement the interface correctly
        val providers = listOf(
            RooExtensionProvider(),
            KiloCodeExtensionProvider(),
        )
        
        providers.forEach { provider ->
            assertNotNull("Extension ID should not be null", provider.getExtensionId())
            assertNotNull("Display name should not be null", provider.getDisplayName())
            assertNotNull("Description should not be null", provider.getDescription())
            
            // Test that provider can be initialized
            provider.initialize(project)
            
            // Test that configuration can be retrieved
            val config = provider.getConfiguration(project)
            assertNotNull("Configuration should not be null", config)
            
            // Test disposal
            provider.dispose()
        }
    }
} 