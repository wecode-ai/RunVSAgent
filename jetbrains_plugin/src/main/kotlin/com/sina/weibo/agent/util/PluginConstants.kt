// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.util

object PluginConstants {
    const val PLUGIN_ID = "RunVSAgent"
    const val NODE_MODULES_PATH = "node_modules"
    const val EXTENSION_ENTRY_FILE = "extension.js"
    const val RUNTIME_DIR = "runtime"

    /**
     * Configuration file constants
     */
    object ConfigFiles {
        /**
         * Main configuration file name
         */
        const val MAIN_CONFIG_FILE = ".vscode-agent"
        
        /**
         * Extension-specific configuration file prefix
         */
        const val EXTENSION_CONFIG_PREFIX = ".vscode-agent."
        
        /**
         * Extension type configuration key
         */
        const val EXTENSION_TYPE_KEY = "extension.type"
        
        /**
         * Debug mode configuration key
         */
        const val DEBUG_MODE_KEY = "debug.mode"
        
        /**
         * Debug resource configuration key
         */
        const val DEBUG_RESOURCE_KEY = "debug.resource"
        
        /**
         * Get user home directory for configuration storage
         */
        fun getUserConfigDir(): String {
            return System.getProperty("user.home") + "/.run-vs-agent"
        }
        
        /**
         * Get main configuration file path in user home directory
         */
        fun getMainConfigPath(): String {
            return getUserConfigDir() + "/" + MAIN_CONFIG_FILE
        }
        
        /**
         * Get extension configuration file path in user home directory
         */
        fun getExtensionConfigPath(extensionId: String): String {
            return getUserConfigDir() + "/" + EXTENSION_CONFIG_PREFIX + extensionId
        }
        
        /**
         * Get extension ID from extension config filename
         */
        fun getExtensionIdFromFilename(filename: String): String? {
            return if (filename.startsWith(EXTENSION_CONFIG_PREFIX)) {
                filename.substring(EXTENSION_CONFIG_PREFIX.length)
            } else null
        }
        
        /**
         * Check if filename is an extension config file
         */
        fun isExtensionConfigFile(filename: String): Boolean {
            return filename.startsWith(EXTENSION_CONFIG_PREFIX) && filename != MAIN_CONFIG_FILE
        }
    }
}