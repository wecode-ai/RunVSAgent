// Copyright 2009-2025 Weibo, Inc.
// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.core

import com.google.gson.Gson
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.application.ApplicationInfo
import com.sina.weibo.agent.editor.EditorAndDocManager
import com.sina.weibo.agent.ipc.NodeSocket
import com.sina.weibo.agent.ipc.PersistentProtocol
import com.sina.weibo.agent.ipc.proxy.ResponsiveState
import com.sina.weibo.agent.util.PluginConstants
import com.sina.weibo.agent.util.PluginResourceUtil
import com.sina.weibo.agent.util.URI
import com.sina.weibo.agent.workspace.WorkspaceFileChangeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.net.Socket
import java.nio.channels.SocketChannel
import java.nio.file.Paths
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.sina.weibo.agent.extensions.core.ExtensionManager as GlobalExtensionManager
import com.sina.weibo.agent.extensions.config.ExtensionProvider
import com.sina.weibo.agent.extensions.config.ExtensionMetadata
import java.io.File

/**
 * Extension host manager, responsible for communication with extension processes.
 * Handles Ready and Initialized messages from extension processes.
 */
class ExtensionHostManager : Disposable {
    companion object {
        val LOG = Logger.getInstance(ExtensionHostManager::class.java)
    }

    private val project: Project
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
     // Communication protocol
    private var nodeSocket: NodeSocket
    private var protocol: PersistentProtocol? = null
    
     // RPC manager
    private var rpcManager: RPCManager? = null
    
     // Extension manager
    private var extensionManager: ExtensionManager? = null
    
    // Current extension provider
    private var currentExtensionProvider: ExtensionProvider? = null
    
    // Extension identifier
    private var extensionIdentifier: String? = null
    
     // JSON serialization
    private val gson = Gson()
    
     // Last diagnostic log time
    private var lastDiagnosticLogTime = 0L

    private var  projectPath: String? = null
    
     // Support Socket constructor
    constructor(clientSocket: Socket, projectPath: String,project: Project) {
        clientSocket.tcpNoDelay = true
        this.nodeSocket = NodeSocket(clientSocket, "extension-host")
        this.projectPath = projectPath
        this.project = project
    }
     // Support SocketChannel constructor
    constructor(clientChannel: SocketChannel, projectPath: String , project: Project) {
        this.nodeSocket = NodeSocket(clientChannel, "extension-host")
        this.projectPath = projectPath
        this.project = project
    }
    
    /**
     * Start communication with the extension process.
     */
    fun start() {
        try {
            // Get current extension provider from global extension manager
            val globalExtensionManager = GlobalExtensionManager.getInstance(project)
            currentExtensionProvider = globalExtensionManager.getCurrentProvider()
            if (currentExtensionProvider == null) {
                LOG.error("No extension provider available")
                dispose()
                return
            }
            
            // Initialize extension manager
            extensionManager = ExtensionManager()
            
            // Get extension configuration
            val extensionConfig = currentExtensionProvider!!.getConfiguration(project)
            
            // Get extension path from configuration
            val extensionPath = getExtensionPath(extensionConfig)
            
            if (extensionPath != null && File(extensionPath).exists()) {
                            // Register extension using configuration
            val extensionDesc = extensionManager!!.registerExtension(extensionPath, extensionConfig)
                extensionIdentifier = extensionDesc.identifier.value
                LOG.info("Registered extension: ${currentExtensionProvider!!.getExtensionId()}")
            } else {
                LOG.error("Extension path not found: $extensionPath")
                dispose()
                return
            }
            
            // Create protocol
            protocol = PersistentProtocol(
                PersistentProtocol.PersistentProtocolOptions(
                    socket = nodeSocket,
                    initialChunk = null,
                    loadEstimator = null,
                    sendKeepAlive = true
                ),
                this::handleMessage
            )

            LOG.info("ExtensionHostManager started successfully with extension: ${currentExtensionProvider!!.getExtensionId()}")
        } catch (e: Exception) {
            LOG.error("Failed to start ExtensionHostManager", e)
            dispose()
        }
    }
    
    /**
     * Get RPC responsive state.
     * @return Responsive state, or null if RPC manager is not initialized.
     */
    fun getResponsiveState(): ResponsiveState? {
        val currentTime = System.currentTimeMillis()
         // Limit diagnostic log frequency, at most once every 60 seconds
        val shouldLogDiagnostics = currentTime - lastDiagnosticLogTime > 60000
        if (rpcManager == null) {
            if (shouldLogDiagnostics) {
                LOG.debug("Unable to get responsive state: RPC manager is not initialized")
                lastDiagnosticLogTime = currentTime
            }
            return null
        }
         // Log connection diagnostic information
        if (shouldLogDiagnostics) {
            val socketInfo = buildString {
                append("NodeSocket: ")
                append(if (nodeSocket.isClosed()) "closed" else "active")
                append(", input stream: ")
                append(if (nodeSocket.isInputClosed()) "closed" else "normal")
                append(", output stream: ")
                append(if (nodeSocket.isOutputClosed()) "closed" else "normal")
                append(", disposed=")
                append(nodeSocket.isDisposed())
            }
            
            val protocolInfo = protocol?.let { proto ->
                "Protocol: ${if (proto.isDisposed()) "disposed" else "active"}"
            } ?: "Protocol is null"
            LOG.debug("Connection diagnostics: $socketInfo, $protocolInfo")
            lastDiagnosticLogTime = currentTime
        }
        return rpcManager?.getRPCProtocol()?.responsiveState
    }
    
    /**
     * Handle messages from the extension process.
     */
    private fun handleMessage(data: ByteArray) {
         // Check if data is a single-byte message (extension host protocol message)
        if (data.size == 1) {
             // Try to parse as extension host message type

            when (ExtensionHostMessageType.fromData(data)) {
                ExtensionHostMessageType.Ready -> handleReadyMessage()
                ExtensionHostMessageType.Initialized -> handleInitializedMessage()
                ExtensionHostMessageType.Terminate -> LOG.info("Received Terminate message")
                null -> LOG.debug("Received unknown message type: ${data.contentToString()}")
            }
        } else {
            LOG.debug("Received message with length ${data.size}, not handling as extension host message")
        }
    }
    
    /**
     * Handle Ready message, send initialization data.
     */
    private fun handleReadyMessage() {
        LOG.info("Received Ready message from extension host")
        
        try {
             // Build initialization data
            val initData = createInitData()
            LOG.info("handleReadyMessage createInitData: ${initData}")
            
             // Send initialization data
            val jsonData = gson.toJson(initData).toByteArray()

            protocol?.send(jsonData)
            LOG.info("Sent initialization data to extension host")
        } catch (e: Exception) {
            LOG.error("Failed to handle Ready message", e)
        }
    }
    
    /**
     * Handle Initialized message, create RPC manager and activate plugin.
     */
    private fun handleInitializedMessage() {
        LOG.info("Received Initialized message from extension host")
        
        try {
            // Get protocol
            val protocol = this.protocol ?: throw IllegalStateException("Protocol is not initialized")
            val extensionManager = this.extensionManager ?: throw IllegalStateException("ExtensionManager is not initialized")
            val currentProvider = this.currentExtensionProvider ?: throw IllegalStateException("Extension provider is not initialized")

            // Create RPC manager
            rpcManager = RPCManager(protocol, extensionManager, null, project)

            // Start initialization process
            rpcManager?.startInitialize()

            // Start file monitoring
            project.getService(WorkspaceFileChangeManager::class.java)
            project.getService(EditorAndDocManager::class.java).initCurrentIdeaEditor()
            
            // Activate extension
            val extensionId = extensionIdentifier ?: throw IllegalStateException("Extension identifier is not initialized")
            extensionManager.activateExtension(extensionId, rpcManager!!.getRPCProtocol())
                .whenComplete { _, error ->
                    if (error != null) {
                        LOG.error("Failed to activate extension: ${currentProvider.getExtensionId()}", error)
                    } else {
                        LOG.info("Extension activated successfully: ${currentProvider.getExtensionId()}")
                    }
                }

            LOG.info("Initialized extension host")
        } catch (e: Exception) {
            LOG.error("Failed to handle Initialized message", e)
        }
    }
    
    /**
     * Create initialization data.
     * Corresponds to the initData object in main.js.
     */
    private fun createInitData(): Map<String, Any?> {
        val pluginDir = getPluginDir()
        val basePath = projectPath
        
        return mapOf(
            "commit" to "development",
            "version" to getIDEVersion(),
            "quality" to null,
            "parentPid" to ProcessHandle.current().pid(),
            "environment" to mapOf(
                "isExtensionDevelopmentDebug" to false,
                "appName" to getCurrentIDEName(),
                "appHost" to "node",
                "appLanguage" to "en",
                "appUriScheme" to "vscode",
                "appRoot" to uriFromPath(pluginDir),
                "globalStorageHome" to uriFromPath(Paths.get(System.getProperty("user.home"),".roo-cline", "globalStorage").toString()),
                "workspaceStorageHome" to uriFromPath(Paths.get(System.getProperty("user.home"),".roo-cline", "workspaceStorage").toString()),
                "extensionDevelopmentLocationURI" to null,
                "extensionTestsLocationURI" to null,
                "useHostProxy" to false,
                "skipWorkspaceStorageLock" to false,
                "isExtensionTelemetryLoggingOnly" to false
            ),
            "workspace" to mapOf(
                "id" to "intellij-workspace",
                "name" to "IntelliJ Workspace",
                "transient" to false,
                "configuration" to null,
                "isUntitled" to false
            ),
            "remote" to mapOf(
                "authority" to null,
                "connectionData" to null,
                "isRemote" to false
            ),
            "extensions" to mapOf<String, Any>(
                "versionId" to 1,
                "allExtensions" to (extensionManager?.getAllExtensionDescriptions() ?: emptyList<Any>()),
                "myExtensions" to (extensionManager?.getAllExtensionDescriptions()?.map { it.identifier } ?: emptyList<Any>()),
                "activationEvents" to (extensionManager?.getAllExtensionDescriptions()?.associate { ext ->
                    ext.identifier.value to (ext.activationEvents ?: emptyList<String>())
                } ?: emptyMap())
            ),
            "telemetryInfo" to mapOf(
                "sessionId" to "intellij-session",
                "machineId" to "intellij-machine",
                "sqmId" to "",
                "devDeviceId" to "",
                "firstSessionDate" to java.time.Instant.now().toString(),
                "msftInternal" to false
            ),
            "logLevel" to 0, // Info level
            "loggers" to emptyList<Any>(),
            "logsLocation" to uriFromPath(Paths.get(pluginDir, "logs").toString()),
            "autoStart" to true,
            "consoleForward" to mapOf(
                "includeStack" to false,
                "logNative" to false
            ),
            "uiKind" to 1 // Desktop
        )
    }
    
    /**
     * Get current IDE name.
     */
    private fun getCurrentIDEName(): String {
        val applicationInfo = ApplicationInfo.getInstance()
         // Get product code, which is the main identifier for distinguishing IDEs
        val productCode = applicationInfo.build.productCode
        val fullName = applicationInfo.fullApplicationName
        
        val ideName = when (productCode) {
            "IC" -> "IntelliJ IDEA"
            "IU" -> "IntelliJ IDEA"  
            "AS" -> "Android Studio"
            "AI" -> "Android Studio"
            "WS" -> "WebStorm"
            "PS" -> "PhpStorm"
            "PY" -> "PyCharm Professional"
            "PC" -> "PyCharm Community"
            "GO" -> "GoLand"
            "CL" -> "CLion"
            "RD" -> "Rider"
            "RM" -> "RubyMine"
            "DB" -> "DataGrip"
            "DS" -> "DataSpell"
            else -> if (fullName?.contains("Android Studio") == true) "Android Studio" else "JetBrains"
        }
        LOG.info("Get IDE name, productCode: $productCode ideName: $ideName fullName: $fullName")
        return ideName
    }
    
    /**
     * Get current IDE version.
     */
    private fun getIDEVersion(): String {
        val applicationInfo = ApplicationInfo.getInstance()
        val version = applicationInfo.shortVersion ?: "1.0.0"
        LOG.info("Get IDE version: $version")

        val pluginVersion = PluginManagerCore.getPlugin(PluginId.getId(PluginConstants.PLUGIN_ID))?.version
        if (pluginVersion != null) {
            val fullVersion = "$version, $pluginVersion"
            LOG.info("Get IDE version and plugin version: $fullVersion")
            return fullVersion
        }

        return version
    }
    
    /**
     * Get plugin directory.
     */
    private fun getPluginDir(): String {
        return PluginResourceUtil.getResourcePath(PluginConstants.PLUGIN_ID, "")
            ?: throw IllegalStateException("Unable to get plugin directory")
    }
    
    /**
     * Get extension path from configuration
     */
    private fun getExtensionPath(extensionConfig: ExtensionMetadata): String? {
        // First check project paths
        val projectPath = project.basePath
        val homeDir = System.getProperty("user.home")
        if (projectPath != null) {
            val possiblePaths = listOf(
                "$homeDir/.run-vs-agent/plugins/${extensionConfig.getCodeDir()}"
            )
            
            val foundPath = possiblePaths.find { File(it).exists() }
            if (foundPath != null) {
                return foundPath
            }
        }
        
        // Then check plugin resources (for built-in extensions)
        try {
            val pluginResourcePath = com.sina.weibo.agent.util.PluginResourceUtil.getResourcePath(
                com.sina.weibo.agent.util.PluginConstants.PLUGIN_ID, 
                extensionConfig.getCodeDir()
            )
            if (pluginResourcePath != null && File(pluginResourcePath).exists()) {
                return pluginResourcePath
            }
        } catch (e: Exception) {
            LOG.warn("Failed to get plugin resource path for extension: ${extensionConfig.getCodeDir()}", e)
        }
        
        // For development/testing, return a default path
        // This allows the extension to work even without the actual extension files
        val defaultPath = projectPath?.let { "$it/${extensionConfig.getCodeDir()}" } ?: "/tmp/${extensionConfig.getCodeDir()}"
        LOG.info("Using default extension path: $defaultPath")
        return defaultPath
    }
    
    /**
     * Create URI object.
     */
    private fun uriFromPath(path: String): URI {
        return URI.file(path)
    }
    
    /**
     * Resource disposal.
     */
    override fun dispose() {
        LOG.info("Disposing ExtensionHostManager")
        
        // Cancel coroutines
        coroutineScope.cancel()
        
        // Release RPC manager
        rpcManager = null
        
        // Release protocol
        protocol?.dispose()
        protocol = null
        
        // Release socket
        nodeSocket.dispose()

        LOG.info("ExtensionHostManager disposed")
    }
} 