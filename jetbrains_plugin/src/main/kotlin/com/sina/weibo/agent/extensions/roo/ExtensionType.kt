// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.extensions.roo

/**
 * Extension type enum for Roo Code
 * Defines different types of extensions that can be supported
 */
enum class ExtensionType(val code: String, val displayName: String, val description: String) {
    ROO_CODE("roo-code", "Roo Code", "AI-powered code assistant"),
    COPILOT("copilot", "GitHub Copilot", "AI pair programming assistant"),
    CLAUDE("claude", "Claude", "Anthropic's AI assistant"),
    CUSTOM("custom", "Custom Extension", "Custom AI extension");
    
    companion object {
        /**
         * Get extension type by code
         * @param code Extension code
         * @return Extension type or null if not found
         */
        fun fromCode(code: String): ExtensionType? {
            return values().find { it.code == code }
        }
        
        /**
         * Get default extension type
         * @return Default extension type
         */
        fun getDefault(): ExtensionType {
            return ROO_CODE
        }
        
        /**
         * Get all supported extension types
         * @return List of all extension types
         */
        fun getAllTypes(): List<ExtensionType> {
            return values().toList()
        }
    }
} 