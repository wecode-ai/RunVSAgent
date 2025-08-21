// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.extensions.core

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.sina.weibo.agent.theme.ThemeManager.Companion.getThemeResourceDir
import com.sina.weibo.agent.util.PluginConstants.ConfigFiles.getUserConfigDir
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.ZipInputStream
import java.io.FileInputStream
import java.io.BufferedInputStream

/**
 * VSIX file manager for extensions
 * Handles VSIX file installation, management, and resource path resolution
 */
class VsixManager {
    
    companion object {
        private val LOG = Logger.getInstance(VsixManager::class.java)

        /**
         * Get VSIX manager instance
         */
        fun getInstance(): VsixManager = VsixManager()
        
        /**
         * Get base directory for VSIX installations
         */
        fun getBaseDirectory(): String {
            return "${getUserConfigDir()}/plugins"
        }
    }
    
    /**
     * Install VSIX file for an extension
     * Supports overwriting existing installations
     */
    fun installVsix(vsixFile: File, extensionId: String): Boolean {
        return try {
            val targetDir = getExtensionDirectory(extensionId)
            LOG.info("Installing VSIX for extension $extensionId to $targetDir")
            
            // Check if extension already exists and log the action
            val existingInstallation = hasVsixInstallation(extensionId)
            if (existingInstallation) {
                LOG.info("Extension $extensionId already exists, will overwrite existing installation")
            }
            
            // Create target directory (will be cleaned in extractVsixFile if exists)
            val targetPath = Paths.get(targetDir)
            Files.createDirectories(targetPath)
            
            // Extract VSIX file (this will automatically clean existing content)
            val success = extractVsixFile(vsixFile, targetDir)
            if (success) {
                if (existingInstallation) {
                    LOG.info("VSIX installation updated successfully for extension $extensionId")
                } else {
                    LOG.info("VSIX installation completed successfully for extension $extensionId")
                }
            } else {
                LOG.error("VSIX extraction failed for extension $extensionId")
            }
            success
        } catch (e: Exception) {
            LOG.error("Failed to install VSIX for extension $extensionId", e)
            false
        }
    }
    
    /**
     * Get extension directory path
     */
    fun getExtensionDirectory(extensionId: String): String {
        return "${getBaseDirectory()}/$extensionId"
    }
    
    /**
     * Check if extension has VSIX installation
     */
    fun hasVsixInstallation(extensionId: String): Boolean {
        val extensionDir = getExtensionDirectory(extensionId)
        val dir = File(extensionDir)
        return dir.exists() && dir.isDirectory && dir.listFiles()?.isNotEmpty() == true
    }
    
    /**
     * Get VSIX installation path for extension
     */
    fun getVsixInstallationPath(extensionId: String): String? {
        return if (hasVsixInstallation(extensionId)) {
            getExtensionDirectory(extensionId)
        } else {
            null
        }
    }
    
    /**
     * Uninstall VSIX for an extension
     */
    fun uninstallVsix(extensionId: String): Boolean {
        return try {
            val extensionDir = getExtensionDirectory(extensionId)
            val dir = File(extensionDir)
            if (dir.exists()) {
                deleteDirectory(dir)
                LOG.info("VSIX uninstalled for extension $extensionId")
                true
            } else {
                LOG.info("No VSIX installation found for extension $extensionId")
                true
            }
        } catch (e: Exception) {
            LOG.error("Failed to uninstall VSIX for extension $extensionId", e)
            false
        }
    }
    
    /**
     * List all installed VSIX extensions
     */
    fun listInstalledExtensions(): List<String> {
        val baseDir = File(getBaseDirectory())
        if (!baseDir.exists() || !baseDir.isDirectory) {
            return emptyList()
        }
        
        return baseDir.listFiles()
            ?.filter { it.isDirectory && it.listFiles()?.isNotEmpty() == true }
            ?.map { it.name }
            ?: emptyList()
    }
    
    /**
     * Extract VSIX file to target directory
     * First extracts to temp directory, then moves only the extension directory contents
     */
    private fun extractVsixFile(vsixFile: File, targetDir: String): Boolean {
        var tempDir: File? = null
        
        return try {
            // Create temporary directory for extraction
            tempDir = Files.createTempDirectory("vsix-extract-").toFile()
            LOG.debug("Created temporary directory: ${tempDir.absolutePath}")
            
            // Extract VSIX file to temp directory (extract everything)
            ZipInputStream(BufferedInputStream(FileInputStream(vsixFile))).use { zis ->
                var entry = zis.nextEntry
                var extractedCount = 0
                
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val entryPath = tempDir!!.toPath().resolve(entry.name)
                        
                        // Create parent directories if needed
                        val parentDir = entryPath.parent
                        if (parentDir != null) {
                            Files.createDirectories(parentDir)
                        }
                        
                        // Extract file
                        Files.copy(zis, entryPath)
                        extractedCount++
                    }
                    entry = zis.nextEntry
                }
                
                LOG.debug("Extracted $extractedCount files to temporary directory")
            }
            
            // Check if extension directory exists in temp directory
            val extensionDir = File(tempDir, "extension")
            if (!extensionDir.exists() || !extensionDir.isDirectory) {
                LOG.error("Extension directory not found in VSIX file")
                return false
            }
            
            // Clear target directory if it exists
            val targetPath = Paths.get(targetDir)
            if (Files.exists(targetPath)) {
                deleteDirectory(targetPath.toFile())
            }
            Files.createDirectories(targetPath)
            
            // Move extension directory contents to target directory
            val extensionFiles = extensionDir.listFiles()
            if (extensionFiles == null || extensionFiles.isEmpty()) {
                LOG.warn("Extension directory is empty")
                return false
            }
            
            var movedCount = 0
            for (file in extensionFiles) {
                val targetFile = targetPath.resolve(file.name).toFile()
                
                if (file.isDirectory) {
                    // Move directory recursively
                    moveDirectory(file, targetFile)
                } else {
                    // Move file
                    Files.move(file.toPath(), targetFile.toPath())
                }
                movedCount++
            }
            
            LOG.info("VSIX extraction completed. Moved $movedCount items from extension directory to: $targetDir")
            
            // Copy themes to integrations/theme/default-themes directory
            copyThemesToIntegrations(targetDir)
            
            // Verify that we actually moved some files
            val targetFiles = targetPath.toFile().listFiles()
            if (targetFiles == null || targetFiles.isEmpty()) {
                LOG.warn("No files were moved to target directory")
                return false
            }
            
            true
        } catch (e: Exception) {
            LOG.error("Failed to extract VSIX file", e)
            false
        } finally {
            // Clean up temporary directory
            tempDir?.let { temp ->
                try {
                    deleteDirectory(temp)
                    LOG.debug("Cleaned up temporary directory: ${temp.absolutePath}")
                } catch (e: Exception) {
                    LOG.warn("Failed to clean up temporary directory", e)
                }
            }
        }
    }
    
    /**
     * Move directory recursively
     */
    private fun moveDirectory(source: File, target: File) {
        if (!target.exists()) {
            target.mkdirs()
        }
        
        val sourceFiles = source.listFiles()
        if (sourceFiles != null) {
            for (file in sourceFiles) {
                val targetFile = File(target, file.name)
                
                if (file.isDirectory) {
                    moveDirectory(file, targetFile)
                } else {
                    Files.move(file.toPath(), targetFile.toPath())
                }
            }
        }
        
        // Remove empty source directory
        source.delete()
    }
    
    /**
     * Copy themes to integrations/theme/default-themes directory
     */
    private fun copyThemesToIntegrations(extensionDir: String) {
        try {
            // Get plugin themes directory
            val pluginThemesDir = getPluginThemesDirectory()
            if (pluginThemesDir == null) {
                LOG.warn("Plugin themes directory not found, skipping themes copy")
                return
            }

            val integrationsThemeDir = getThemeResourceDir(extensionDir)
            if (integrationsThemeDir == null) {
                LOG.warn("Plugin themes directory not found, skipping themes copy")
                return
            }
            
            // Create integrations theme directory
            Files.createDirectories(integrationsThemeDir)
            
            // Copy theme files
            val themeFiles = pluginThemesDir.listFiles { file -> 
                file.isFile && file.extension?.lowercase() == "css"
            }
            
            if (themeFiles != null) {
                var copiedCount = 0
                for (themeFile in themeFiles) {
                    val targetFile = integrationsThemeDir.resolve(themeFile.name).toFile()
                    Files.copy(themeFile.toPath(), targetFile.toPath())
                    copiedCount++
                    LOG.debug("Copied theme file: ${themeFile.name} to integrations directory")
                }
                
                LOG.info("Copied $copiedCount theme files to integrations/theme/default-themes")
            } else {
                LOG.warn("No theme files found in plugin themes directory")
            }
            
        } catch (e: Exception) {
            LOG.warn("Failed to copy themes to integrations directory", e)
        }
    }
    
    /**
     * Get plugin themes directory
     */
    private fun getPluginThemesDirectory(): File? {
        return try {
            // Try to get themes directory from plugin resources
            val pluginThemesPath = com.sina.weibo.agent.util.PluginResourceUtil.getResourcePath(
                com.sina.weibo.agent.util.PluginConstants.PLUGIN_ID,
                "themes"
            )
            
            if (pluginThemesPath != null) {
                val themesDir = File(pluginThemesPath)
                if (themesDir.exists() && themesDir.isDirectory) {
                    return themesDir
                }
            }
            
            // Fallback: try to find themes in current working directory
            val currentDir = File(System.getProperty("user.dir") ?: "")
            val themesDir = File(currentDir, "src/main/resources/themes")
            if (themesDir.exists() && themesDir.isDirectory) {
                return themesDir
            }
            
            LOG.warn("Plugin themes directory not found")
            null
        } catch (e: Exception) {
            LOG.warn("Failed to get plugin themes directory", e)
            null
        }
    }
    
    /**
     * Delete directory and all its contents
     */
    private fun deleteDirectory(dir: File) {
        if (dir.isDirectory) {
            dir.listFiles()?.forEach { file ->
                deleteDirectory(file)
            }
        }
        dir.delete()
    }
    
    /**
     * Get extension resource path with VSIX support
     * Priority: Project path > VSIX installation > Plugin resources
     */
    fun getExtensionResourcePath(
        extensionId: String,
        codeDir: String,
        projectPath: String?
    ): String? {
        // First check project paths
        if (projectPath != null) {
            val possiblePaths = listOf(
                "$projectPath/$codeDir",
                "$projectPath/../$codeDir",
                "$projectPath/../../$codeDir"
            )
            
            for (path in possiblePaths) {
                if (File(path).exists()) {
                    LOG.debug("Found extension resources in project path: $path")
                    return path
                }
            }
        }
        
        // Then check VSIX installation
        val vsixPath = getVsixInstallationPath(extensionId)
        if (vsixPath != null) {
            // Since we now extract extension contents directly to the target directory,
            // we don't need to check for an 'extension' subdirectory
            if (File(vsixPath).exists()) {
                LOG.debug("Found extension resources in VSIX installation: $vsixPath")
                return vsixPath
            }
        }
        
        // Finally check plugin resources
        try {
            val pluginPath = com.sina.weibo.agent.util.PluginResourceUtil.getResourcePath(
                com.sina.weibo.agent.util.PluginConstants.PLUGIN_ID,
                codeDir
            )
            if (pluginPath != null && File(pluginPath).exists()) {
                LOG.debug("Found extension resources in plugin: $pluginPath")
                return pluginPath
            }
        } catch (e: Exception) {
            LOG.debug("Failed to get plugin resource path for extension: $extensionId", e)
        }
        
        LOG.debug("No extension resources found for extension: $extensionId")
        return null
    }
}
