// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.core

import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.SystemInfo
import com.sina.weibo.agent.plugin.DEBUG_MODE
import com.sina.weibo.agent.plugin.WecoderPluginService
import com.sina.weibo.agent.util.PluginResourceUtil
import com.sina.weibo.agent.util.ProxyConfigUtil
import java.io.File
import java.util.concurrent.TimeUnit
import com.sina.weibo.agent.util.ExtensionUtils
import com.sina.weibo.agent.util.PluginConstants
import com.sina.weibo.agent.util.NotificationUtil
import com.sina.weibo.agent.util.NodeVersionUtil
import com.sina.weibo.agent.util.NodeVersion

/**
 * Extension process manager
 * Responsible for starting and managing extension processes
 */
class ExtensionProcessManager : Disposable {
    companion object {
        // Node modules path
        private const val NODE_MODULES_PATH = PluginConstants.NODE_MODULES_PATH
        
        // Extension process entry file
        private const val EXTENSION_ENTRY_FILE = PluginConstants.EXTENSION_ENTRY_FILE
        
        // Runtime directory
        private const val RUNTIME_DIR = PluginConstants.RUNTIME_DIR
        
        // Plugin ID
        private const val PLUGIN_ID = PluginConstants.PLUGIN_ID
        
        // Minimum required Node.js version
        private val MIN_REQUIRED_NODE_VERSION = NodeVersion(20, 6, 0, "20.6.0")
    }
    
    private val LOG = Logger.getInstance(ExtensionProcessManager::class.java)
    
    // Extension process
    private var process: Process? = null
    
    // Process monitor thread
    private var monitorThread: Thread? = null
    
    // Whether running
    @Volatile
    private var isRunning = false
    
    /**
     * Start extension process
     * @param portOrPath Socket server port (Int) or UDS path (String)
     * @return Whether started successfully
     */
    fun start(portOrPath: Any?): Boolean {
        if (isRunning) {
            LOG.info("Extension process is already running")
            return true
        }
        val isUds = portOrPath is String
        if (!ExtensionUtils.isValidPortOrPath(portOrPath)) {
            LOG.error("Invalid socket info: $portOrPath")
            return false
        }
        
        try {
            // Prepare Node.js executable path
            val nodePath = findNodeExecutable()
            if (nodePath == null) {
                LOG.error("Failed to find Node.js executable")
                
                // Show notification to prompt user to install Node.js
                NotificationUtil.showError(
                    "Node.js environment missing",
                    "Node.js environment not detected, please install Node.js and try again. Recommended version: $MIN_REQUIRED_NODE_VERSION or higher."
                )
                
                return false
            }
            
            // Check Node.js version
            val nodeVersion = NodeVersionUtil.getNodeVersion(nodePath)
            if (!NodeVersionUtil.isVersionSupported(nodeVersion, MIN_REQUIRED_NODE_VERSION)) {
                LOG.error("Node.js version is not supported: $nodeVersion, required: $MIN_REQUIRED_NODE_VERSION")

                NotificationUtil.showError(
                    "Node.js version too low",
                    "Current Node.js($nodePath) version is $nodeVersion, please upgrade to $MIN_REQUIRED_NODE_VERSION or higher for better compatibility."
                )
                
                return false
            }
            
            // Prepare extension process entry file path
            val extensionPath = findExtensionEntryFile()
            if (extensionPath == null) {
                LOG.error("Failed to find extension entry file")
                return false
            }

            val nodeModulesPath = findNodeModulesPath()
            if (nodeModulesPath == null) {
                LOG.error("Failed to find node_modules directory")
                return false
            }
            
            LOG.info("Starting extension process with node: $nodePath, entry: $extensionPath")

            val envVars = HashMap<String, String>(System.getenv())
            
            // Build complete PATH
            envVars["PATH"] = buildEnhancedPath(envVars, nodePath)
            LOG.info("Enhanced PATH for ${SystemInfo.getOsNameAndVersion()}: ${envVars["PATH"]}")
            
            // Add key environment variables
            if (isUds) {
                envVars["VSCODE_EXTHOST_IPC_HOOK"] = portOrPath.toString()
            }else{
                envVars["VSCODE_EXTHOST_WILL_SEND_SOCKET"] = "1"
                envVars["VSCODE_EXTHOST_SOCKET_HOST"] = "127.0.0.1"
                envVars["VSCODE_EXTHOST_SOCKET_PORT"] = portOrPath.toString()
            }

            // Build command line arguments
            val commandArgs = mutableListOf(
                nodePath,
                "--experimental-global-webcrypto",
                "--no-deprecation",
//                "--trace-uncaught",
                extensionPath,
                "--vscode-socket-port=${envVars["VSCODE_EXTHOST_SOCKET_PORT"]}",
                "--vscode-socket-host=${envVars["VSCODE_EXTHOST_SOCKET_HOST"]}",
                "--vscode-will-send-socket=${envVars["VSCODE_EXTHOST_WILL_SEND_SOCKET"]}"
            )
            
            // Get and set proxy configuration
            try {
                val proxyEnvVars = ProxyConfigUtil.getProxyEnvVarsForProcessStart()
                
                // Add proxy environment variables
                envVars.putAll(proxyEnvVars)
                
                // Log proxy configuration if used
                if (proxyEnvVars.isNotEmpty()) {
                    LOG.info("Applied proxy configuration for process startup")
                }
            } catch (e: Exception) {
                LOG.warn("Failed to configure proxy settings", e)
            }
            
            // Create process builder
            val builder = ProcessBuilder(commandArgs)

            // Print environment variables
            LOG.info("Environment variables:")
            envVars.forEach { (key, value) ->
                LOG.info("  $key = $value")
            }
            builder.environment().putAll(envVars)

            // Redirect error stream to standard output
            builder.redirectErrorStream(true)
            
            // Start process
            process = builder.start()
            
            // Start monitor thread
            monitorThread = Thread {
                monitorProcess()
            }.apply {
                name = "ExtensionProcessMonitor"
                isDaemon = true
                start()
            }
            
            isRunning = true
            LOG.info("Extension process started")
            return true
        } catch (e: Exception) {
            LOG.error("Failed to start extension process", e)
            stopInternal()
            return false
        }
    }
    
    /**
     * Monitor extension process
     */
    private fun monitorProcess() {
        val proc = process ?: return
        
        try {
            // Start log reading thread
            val logThread = Thread {
                proc.inputStream.bufferedReader().use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        LOG.info("Extension process: $line")
                    }
                }
            }
            logThread.name = "ExtensionProcessLogger"
            logThread.isDaemon = true
            logThread.start()
            
            // Wait for process to end
            try {
                val exitCode = proc.waitFor()
                LOG.info("Extension process exited with code: $exitCode")
            } catch (e: InterruptedException) {
                LOG.info("Process monitor interrupted")
            }
            
            // Ensure log thread ends
            logThread.interrupt()
            try {
                logThread.join(1000)
            } catch (e: InterruptedException) {
                // Ignore
            }
        } catch (e: Exception) {
            LOG.error("Error monitoring extension process", e)
        } finally {
            synchronized(this) {
                if (process === proc) {
                    isRunning = false
                    process = null
                }
            }
        }
    }
    
    /**
     * Stop extension process
     */
    fun stop() {
        if (!isRunning) {
            return
        }
        
        stopInternal()
    }
    
    /**
     * Internal stop logic
     */
    private fun stopInternal() {
        LOG.info("Stopping extension process")
        
        val proc = process
        if (proc != null) {
            try {
                // Try to close normally
                if (proc.isAlive) {
                    proc.destroy()
                    
                    // Wait for process to end
                    if (!proc.waitFor(5, TimeUnit.SECONDS)) {
                        // Force terminate
                        proc.destroyForcibly()
                        proc.waitFor(2, TimeUnit.SECONDS)
                    }
                }
            } catch (e: Exception) {
                LOG.error("Error stopping extension process", e)
            }
        }
        
        // Interrupt monitor thread
        monitorThread?.interrupt()
        try {
            monitorThread?.join(1000)
        } catch (e: InterruptedException) {
            // Ignore
        }
        
        process = null
        monitorThread = null
        isRunning = false
        
        LOG.info("Extension process stopped")
    }
    
    /**
     * Find Node.js executable
     */
    private fun findNodeExecutable(): String? {
        // First check built-in Node.js
        val resourcesPath = PluginResourceUtil.getResourcePath(PLUGIN_ID, NODE_MODULES_PATH)
        if (resourcesPath != null) {
            val resourceDir = File(resourcesPath)
            if (resourceDir.exists() && resourceDir.isDirectory) {
                val nodeBin = if (SystemInfo.isWindows) {
                    File(resourceDir, "node.exe")
                } else {
                    File(resourceDir, ".bin/node")
                }
                
                if (nodeBin.exists() && nodeBin.canExecute()) {
                    return nodeBin.absolutePath
                }
            }
        }
        
        // Then check system path
        return findExecutableInPath("node")
    }
    
    /**
     * Find executable in system path
     */
    private fun findExecutableInPath(name: String): String? {
        val nodePath = PathEnvironmentVariableUtil.findExecutableInPathOnAnyOS("node")?.absolutePath
        LOG.info("System Node path: $nodePath")
        return nodePath
    }
    
    /**
     * Find extension process entry file
     * @param projectBasePath Current project root path
     */
    fun findExtensionEntryFile(): String? {
        // In debug mode, directly return debug-resources path
        if (WecoderPluginService.getDebugMode() != DEBUG_MODE.NONE) {
            val debugEntry = java.nio.file.Paths.get(WecoderPluginService.getDebugResource(), RUNTIME_DIR, EXTENSION_ENTRY_FILE).normalize().toFile()
            if (debugEntry.exists() && debugEntry.isFile) {
                LOG.info("[DebugMode] Using debug entry file: ${debugEntry.absolutePath}")
                return debugEntry.absolutePath
            } else {
                LOG.warn("[DebugMode] Debug entry file not found: ${debugEntry.absolutePath}")
            }
        }
        // Normal mode
        val resourcesPath = com.sina.weibo.agent.util.PluginResourceUtil.getResourcePath(PLUGIN_ID, "$RUNTIME_DIR/$EXTENSION_ENTRY_FILE")
        if (resourcesPath != null) {
            val resource = java.io.File(resourcesPath)
            if (resource.exists() && resource.isFile) {
                return resourcesPath
            }
        }
        return null
    }
    

    
    /**
     * Find node_modules path
     */
    private fun findNodeModulesPath(): String? {
        val nodePath = PluginResourceUtil.getResourcePath(PLUGIN_ID, NODE_MODULES_PATH)
        if (nodePath != null) {
            val nodeDir = File(nodePath)
            if (nodeDir.exists() && nodeDir.isDirectory) {
                return nodeDir.absolutePath
            }
        }
        return null
    }
    
    /**
     * Build enhanced PATH environment variable
     * @param envVars Environment variable map
     * @param nodePath Node.js executable path
     * @return Enhanced PATH
     */
    private fun buildEnhancedPath(envVars: MutableMap<String, String>, nodePath: String): String {
        // Find current PATH value (Path on Windows)
        val currentPath = envVars.filterKeys { it.equals("PATH", ignoreCase = true) }
            .values.firstOrNull() ?: ""
        
        val pathBuilder = mutableListOf<String>()

        // Simplify: add Node directory to PATH head (npx usually in same dir as node)
        val nodeDir = File(nodePath).parentFile?.absolutePath
        if (nodeDir != null && !currentPath.contains(nodeDir)) {
            pathBuilder.add(nodeDir)
        }

        // Add common paths according to OS
        val commonDevPaths = when {
            SystemInfo.isMac -> listOf(
                "/opt/homebrew/bin",
                "/opt/homebrew/sbin",
                "/usr/local/bin",
                "/usr/local/sbin",
                "${System.getProperty("user.home")}/.local/bin"
            )
            SystemInfo.isWindows -> listOf(
                "C:\\Windows\\System32",
                "C:\\Windows\\SysWOW64",
                "C:\\Windows",
                "C:\\Windows\\System32\\WindowsPowerShell\\v1.0",
                "C:\\Program Files\\PowerShell\\7",
                "C:\\Program Files (x86)\\PowerShell\\7"
            )
            else -> emptyList()
        }

        // Add existing paths
        commonDevPaths.forEach { path ->
            if (File(path).exists() && !currentPath.contains(path)) {
                pathBuilder.add(path)
                LOG.info("Add path to PATH: $path")
            } else if (!File(path).exists()) {
                LOG.warn("Path does not exist, skip: $path")
            }
        }

        // Keep original PATH
        if (currentPath.isNotEmpty()) {
            pathBuilder.add(currentPath)
        }

        return pathBuilder.joinToString(File.pathSeparator)
    }
    
    /**
     * Whether running
     */
    fun isRunning(): Boolean {
        return isRunning && process?.isAlive == true
    }
    
    override fun dispose() {
        stop()
    }
}