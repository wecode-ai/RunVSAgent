// Copyright 2009-2025 Weibo, Inc.
// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.core

import com.google.gson.Gson
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.sina.weibo.agent.extensions.config.ExtensionMetadata as ExtensionConfigurationInterface
import com.sina.weibo.agent.ipc.proxy.IRPCProtocol
import com.sina.weibo.agent.util.URI
import com.sina.weibo.agent.util.toCompletableFuture
import com.sina.weibo.agent.extensions.config.ExtensionConfig as RooExtensionConfig
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * Extension manager
 * Responsible for managing extension registration and activation
 */
class ExtensionManager : Disposable {
    companion object {
        val LOG = Logger.getInstance(ExtensionManager::class.java)
    }
    
    // Registered extensions
    private val extensions = ConcurrentHashMap<String, ExtensionDescription>()
    
    // Gson instance
    private val gson = Gson()

    /**
     * Parse extension description information
     * @param extensionPath Extension path
     * @param extensionConfig Extension configuration
     * @return Extension description object
     */
    private fun parseExtensionDescription(extensionPath: String, extensionConfig: RooExtensionConfig): ExtensionDescription {
        LOG.info("Parsing extension: $extensionPath")
        
        // Read package.json file
        val packageJsonPath = Paths.get(extensionPath, "package.json").toString()
        val packageJsonContent = File(packageJsonPath).readText()
        val packageJson = gson.fromJson(packageJsonContent, PackageJson::class.java)
        
        // Create extension identifier using configuration
        val name = "${extensionConfig.publisher}.${packageJson.name}"
        val extensionIdentifier = ExtensionIdentifier(name)
        
        // Create extension description
        return ExtensionDescription(
            id = name,
            identifier = extensionIdentifier,
            name = name,
            displayName = "${extensionConfig.displayName}: ${packageJson.displayName}",
            description = "${extensionConfig.description}: ${packageJson.description}",
            version = packageJson.version ?: extensionConfig.version,
            publisher = extensionConfig.publisher,
            main = packageJson.main ?: extensionConfig.mainFile,
            activationEvents = packageJson.activationEvents ?: extensionConfig.activationEvents,
            extensionLocation = URI.file(extensionPath),
            targetPlatform = "universal", // TargetPlatform.UNIVERSAL
            isBuiltin = false,
            isUserBuiltin = false,
            isUnderDevelopment = false,
            engines = packageJson.engines?.let { 
                mapOf("vscode" to (it.vscode ?: "^1.0.0"))
            } ?: extensionConfig.engines,
            preRelease = false,
            capabilities = extensionConfig.capabilities,
            extensionDependencies = packageJson.extensionDependencies ?: extensionConfig.extensionDependencies,
        )
    }
    
    /**
     * Parse extension description information from new configuration interface
     * @param extensionPath Extension path
     * @param extensionConfig Extension configuration
     * @return Extension description object
     */
    private fun parseExtensionDescriptionFromNewConfig(extensionPath: String, extensionConfig: ExtensionConfigurationInterface): ExtensionDescription {
        LOG.info("Parsing extension: $extensionPath")
        
        // Read package.json file
        val packageJsonPath = Paths.get(extensionPath, "package.json").toString()
        val packageJsonContent = File(packageJsonPath).readText()
        val packageJson = gson.fromJson(packageJsonContent, PackageJson::class.java)
        
        // Create extension identifier using configuration
        val name = "${extensionConfig.getPublisher()}.${packageJson.name}"
        val extensionIdentifier = ExtensionIdentifier(name)
        
        // Create extension description
        return ExtensionDescription(
            id = name,
            identifier = extensionIdentifier,
            name = name,
            displayName = "${extensionConfig.getCodeDir()}: ${packageJson.displayName}",
            description = "${extensionConfig.getCodeDir()}: ${packageJson.description}",
            version = packageJson.version ?: extensionConfig.getVersion(),
            publisher = extensionConfig.getPublisher(),
            main = packageJson.main ?: extensionConfig.getMainFile(),
            activationEvents = packageJson.activationEvents ?: extensionConfig.getActivationEvents(),
            extensionLocation = URI.file(extensionPath),
            targetPlatform = "universal", // TargetPlatform.UNIVERSAL
            isBuiltin = false,
            isUserBuiltin = false,
            isUnderDevelopment = false,
            engines = packageJson.engines?.let { 
                mapOf("vscode" to (it.vscode ?: "^1.0.0"))
            } ?: extensionConfig.getEngines(),
            preRelease = false,
            capabilities = extensionConfig.getCapabilities(),
            extensionDependencies = packageJson.extensionDependencies ?: extensionConfig.getExtensionDependencies(),
        )
    }
    
    /**
     * Get all parsed extension descriptions
     * @return Extension description array
     */
    fun getAllExtensionDescriptions(): List<ExtensionDescription> {
        return extensions.values.toList()
    }
    
    /**
     * Get description information for the specified extension
     * @param extensionId Extension ID
     * @return Extension description object, or null if not found
     */
    fun getExtensionDescription(extensionId: String): ExtensionDescription? {
        return extensions[extensionId]
    }
    
    /**
     * Register extension
     * @param extensionPath Extension path
     * @param extensionConfig Extension configuration
     * @return Extension description object
     */
    fun registerExtension(extensionPath: String, extensionConfig: RooExtensionConfig): ExtensionDescription {
        val extensionDescription = parseExtensionDescription(extensionPath, extensionConfig)
        extensions[extensionDescription.name] = extensionDescription
        LOG.info("Extension registered: ${extensionDescription.name}")
        return extensionDescription
    }
    
    /**
     * Register extension with new configuration interface
     * @param extensionPath Extension path
     * @param extensionConfig Extension configuration
     * @return Extension description object
     */
    fun registerExtension(extensionPath: String, extensionConfig: ExtensionConfigurationInterface): ExtensionDescription {
        val extensionDescription = parseExtensionDescriptionFromNewConfig(extensionPath, extensionConfig)
        extensions[extensionDescription.name] = extensionDescription
        LOG.info("Extension registered: ${extensionDescription.name}")
        return extensionDescription
    }
    
    /**
     * Activate extension
     * @param extensionId Extension ID
     * @param rpcProtocol RPC protocol
     * @return Completion Future
     */
    fun activateExtension(extensionId: String, rpcProtocol: IRPCProtocol): CompletableFuture<Boolean> {
        LOG.info("Activating extension: $extensionId")
        
        try {
            // Get extension description
            val extension = extensions[extensionId]
            if (extension == null) {
                LOG.error("Extension not found: $extensionId")
                val future = CompletableFuture<Boolean>()
                future.completeExceptionally(IllegalArgumentException("Extension not found: $extensionId"))
                return future
            }

            // Create activation parameters
            val activationParams = mapOf(
                "startup" to true,
                "extensionId" to extension.identifier,
                "activationEvent" to "api"
            )

            // Get proxy of ExtHostExtensionServiceShape type
            val extHostService = rpcProtocol.getProxy(ServiceProxyRegistry.ExtHostContext.ExtHostExtensionService)
            
            try {
                // Get LazyPromise instance and convert it to CompletableFuture<Boolean>
                val lazyPromise = extHostService.activate(extension.identifier.value, activationParams)
                
                return lazyPromise.toCompletableFuture<Any?>().thenApply { result ->
                    val boolResult = when (result) {
                        is Boolean -> result
                        else -> false
                    }
                    LOG.info("Extension activation ${if (boolResult) "successful" else "failed"}: $extensionId")
                    boolResult
                }.exceptionally { throwable ->
                    LOG.error("Failed to activate extension: $extensionId", throwable)
                    false
                }
            } catch (e: Exception) {
                LOG.error("Failed to call activate method: $extensionId", e)
                val future = CompletableFuture<Boolean>()
                future.completeExceptionally(e)
                return future
            }
            
        } catch (e: Exception) {
            LOG.error("Failed to activate extension: $extensionId", e)
            val future = CompletableFuture<Boolean>()
            future.completeExceptionally(e)
            return future
        }
    }

    /**
     * Release resources
     */
    override fun dispose() {
        LOG.info("Releasing ExtensionManager resources")
        extensions.clear()
    }
}

/**
 * package.json data class
 * Used for Gson parsing of extension's package.json file
 */
data class PackageJson(
    val name: String,
    val displayName: String? = null,
    val description: String? = null,
    val publisher: String? = null,
    val version: String? = null,
    val engines: Engines? = null,
    val activationEvents: List<String>? = null,
    val main: String? = null,
    val extensionDependencies: List<String>? = null
)

/**
 * Engines data class
 * Used for parsing engines field
 */
data class Engines(
    val vscode: String? = null,
    val node: String? = null
)

/**
 * Extension description
 * Corresponds to IExtensionDescription in VSCode
 */
data class ExtensionDescription(
    val id: String? = null,
    val identifier: ExtensionIdentifier,
    val name: String,
    val displayName: String? = null,
    val description: String? = null,
    val version: String,
    val publisher: String,
    val main: String? = null,
    val activationEvents: List<String>? = null,
    val extensionLocation: URI,
    val targetPlatform: String = "universal",
    val isBuiltin: Boolean = false,
    val isUserBuiltin: Boolean = false,
    val isUnderDevelopment: Boolean = false,
    val engines: Map<String, String>,
    val preRelease: Boolean = false,
    val capabilities: Map<String, Any> = emptyMap(),
    val extensionDependencies: List<String> = emptyList(),
)

/**
 * Convert ExtensionDescription to Map<String, Any?>
 * @return Map containing all properties of ExtensionDescription, where identifier is converted to sid string
 */
fun ExtensionDescription.toMap(): Map<String, Any?> {
    return mapOf(
        "identifier" to this.identifier.value,
        "name" to this.name,
        "displayName" to this.displayName,
        "description" to this.description,
        "version" to this.version,
        "publisher" to this.publisher,
        "main" to this.main,
        "activationEvents" to this.activationEvents,
        "extensionLocation" to this.extensionLocation,
        "targetPlatform" to this.targetPlatform,
        "isBuiltin" to this.isBuiltin,
        "isUserBuiltin" to this.isUserBuiltin,
        "isUnderDevelopment" to this.isUnderDevelopment,
        "engines" to this.engines,
        "preRelease" to this.preRelease,
        "capabilities" to this.capabilities,
        "extensionDependencies" to this.extensionDependencies
    )
} 