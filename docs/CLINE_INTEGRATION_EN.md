## 1. Architecture Overview

- **Extension Provider Interface Layer**: Uses `ExtensionProvider` as the unified contract. Cline implements an extension provider via `ClineExtensionProvider` to handle availability, initialization, metadata retrieval, and disposal.
- **Global Extension Management Layer**: `extensions/core/ExtensionManager` is the project-level global service that registers/selects/switches extension providers and coordinates UI (buttons/context menus) and configuration updates.
- **UI Dynamic Adaptation Layer**: `DynamicButtonManager` and `DynamicContextMenuManager` generate the provider-specific buttons/menus based on the current extension provider. Cline defines its UI through `ClineButtonProvider` and `ClineContextMenuProvider`.
- **Commands/Events and Host Interaction Layer**: Each UI entry triggers `executeCommand("cline.xxx", ...)` to interact with the host; core actions (such as creating a new task) are dispatched to backend logic via command routing.
- **WebView Capability Layer**: Requires WebView to render the frontend UI. Currently, only the “New Task (Plus)” button checks `WebViewManager` availability before executing the command. History/Account/Settings/MCP depend on backend command handling and UI updates.

---

### Architecture Diagram (Mermaid)

```mermaid
graph TD
    subgraph UI[UI Layer]
        BTN[Toolbar Buttons<br/>ClineButtonProvider]
        MENU[Context Menu<br/>ClineContextMenuProvider]
    end

    subgraph Actions[Command Dispatch]
        EXEC[executeCommand cline commands<br/>actions package]
    end

    subgraph ProjectSvc[Project-level Extension Management]
        EM[extensions/core/ExtensionManager]
        DBM[DynamicButtonManager]
        DCM[DynamicContextMenuManager]
    end

    subgraph Provider[Extension Provider]
        CLP[ClineExtensionProvider<br/>initialize/isAvailable/getConfiguration]
    end

    subgraph Core[Core Extension System (VSCode Compatibility Layer)]
        CEM[core/ExtensionManager<br/>register/activate]
        RPC[JSON-RPC / IPC]
        EXTH[ExtHostExtensionService.activate]
    end

    subgraph WebView[WebView Capability]
        WVM[WebViewManager]
        WV[WebView Instances]
    end

    BTN -->|Plus pre-check| WVM
    WVM -->|has latest WebView?| WV
    WV -->|Yes| EXEC
    WVM -. None/Empty .-> BTN

    MENU --> EXEC
    EXEC --> EM
    EM -->|setCurrentProvider/initialize| CLP
    EM --> DBM
    EM --> DCM

    CLP -->|getConfiguration| CEM
    CEM -->|register/activate| RPC --> EXTH

    EXEC -->|UI sync| WVM --> WV
```

---

## 2. Integration Summary and Code Locations

Read this first: to integrate Cline you minimally need to implement/verify these classes and responsibilities.

- Required (the plugin-side trio):
  - `ClineExtensionProvider`: extension identity/description, initialization and resource disposal, availability checks; provide metadata to the core layer via `getConfiguration(project)`.
  - `ClineButtonProvider`: define toolbar buttons (Plus/History/Account/Settings/MCP) and dispatch `cline.*` commands on click; the Plus button checks WebView availability before execution.
  - `ClineContextMenuProvider`: declare context menu visibility policies and optionally return concrete `AnAction` items (currently a placeholder, can be expanded later).

- No new implementation needed but must be wired up:
  - `extensions/core/ExtensionManager`: project-level service to register providers, switch extensions, refresh buttons/menus, update configuration, and broadcast events (Cline is already included).
  - Command registry `com.sina.weibo.agent.actions`: ensure `cline.*` commands are registered and aligned with the frontend WebView.
  - Extension frontend files: `package.json` and `main` under `codeDir` can be provided via user space or built-in plugin resources to satisfy `isAvailable` checks.

- Cline related classes:
  - `jetbrains_plugin/src/main/kotlin/com/sina/weibo/agent/extensions/plugin/cline/ClineExtensionProvider.kt`
  - `jetbrains_plugin/src/main/kotlin/com/sina/weibo/agent/extensions/plugin/cline/ClineButtonProvider.kt`
  - `jetbrains_plugin/src/main/kotlin/com/sina/weibo/agent/extensions/plugin/cline/ClineContextMenuProvider.kt`
- Global extension management:
  - `jetbrains_plugin/src/main/kotlin/com/sina/weibo/agent/extensions/core/ExtensionManager.kt` (project-level management)
- Core (VSCode compatibility layer) extension registration/activation (for deeper activation chain understanding):
  - `jetbrains_plugin/src/main/kotlin/com/sina/weibo/agent/core/ExtensionManager.kt` (parse `package.json`, register and activate)

---

## 3. Key Classes and Responsibilities

- **`ClineExtensionProvider`**
  - Identity/metadata: `getExtensionId()="cline"`, `getDisplayName()`, `getDescription()`
  - Lifecycle: `initialize(project)` for extension configuration and factory initialization; `dispose()` for resource cleanup
  - Availability: `isAvailable(project)` decides availability based on extension file paths or built-in plugin resources
  - Metadata bridge: `getConfiguration(project)` wraps `ExtensionConfiguration.getConfig(ExtensionType.CLINE)` as `ExtensionMetadata` for core registration/activation

```1:30:jetbrains_plugin/src/main/kotlin/com/sina/weibo/agent/extensions/plugin/cline/ClineExtensionProvider.kt
class ClineExtensionProvider : ExtensionProvider {
    
    override fun getExtensionId(): String = "cline"
    
    override fun getDisplayName(): String = "Cline AI"
    
    override fun getDescription(): String = "AI-powered coding assistant with advanced features"
    
    override fun initialize(project: Project) {
        // Initialize cline extension configuration
        val extensionConfig = ExtensionConfiguration.getInstance(project)
        extensionConfig.initialize()
        
        // Initialize extension manager factory if needed
        try {
            val extensionManagerFactory = ExtensionManagerFactory.getInstance(project)
            extensionManagerFactory.initialize()
        } catch (e: Exception) {
            // If ExtensionManagerFactory is not available, continue without it
            // This allows cline to work independently
        }
    }
}
```

- **`ClineButtonProvider`**
  - Defines Cline’s toolbar buttons: New Task, MCP, History, Account, Settings
  - Each button click dispatches its command (e.g., `cline.plusButtonClicked`); the “New Task” button checks WebView availability before execution

```59:76:jetbrains_plugin/src/main/kotlin/com/sina/weibo/agent/extensions/plugin/cline/ClineButtonProvider.kt
override fun actionPerformed(e: AnActionEvent) {
    val logger = Logger.getInstance(this::class.java)
    logger.info("🔍 Cline Plus button clicked, command: cline.plusButtonClicked")
    logger.info("🔍 Project: ${e.project?.name}")
    
    // Check WebView status before executing command
    val project = e.project
    if (project != null) {
        try {
            val webViewManager = project.getService(WebViewManager::class.java)
            if (webViewManager != null) {
                val latestWebView = webViewManager.getLatestWebView()

                if (latestWebView != null) {
                    logger.info("✅ WebView instances available, executing command...")
                    executeCommand("cline.plusButtonClicked", project, hasArgs = false)
                    logger.info("✅ Command executed successfully")
                } else {
                    logger.warn("⚠️ No WebView instances available")
                    Messages.showWarningDialog(
                        project,
                        "No active WebView found. Please ensure the Cline extension is properly initialized.",
                        "WebView Not Available"
                    )
                }
```

- **`ClineContextMenuProvider`**
  - Provides visibility policies for context menu items; currently exposes five categories: Explain/Fix/Improve/AddToContext/NewTask

```44:52:jetbrains_plugin/src/main/kotlin/com/sina/weibo/agent/extensions/plugin/cline/ClineContextMenuProvider.kt
override fun isActionVisible(actionType: ContextMenuActionType): Boolean {
    return when (actionType) {
        ContextMenuActionType.EXPLAIN_CODE,
        ContextMenuActionType.FIX_CODE,
        ContextMenuActionType.IMPROVE_CODE,
        ContextMenuActionType.ADD_TO_CONTEXT,
        ContextMenuActionType.NEW_TASK -> true
        ContextMenuActionType.FIX_LOGIC -> false // Cline doesn't have separate logic fix
    }
}
```

- **Project-level `extensions/core/ExtensionManager`**
  - Registers all providers, sets current provider, switches extensions, drives dynamic configurations for buttons/menus, persists configuration

```116:126:jetbrains_plugin/src/main/kotlin/com/sina/weibo/agent/extensions/core/ExtensionManager.kt
private fun registerExtensionProviders() {
    getAllExtensions().forEach { registerExtensionProvider(it) }
}

fun registerExtensionProvider(provider: ExtensionProvider) {
    extensionProviders[provider.getExtensionId()] = provider
    LOG.info("Registered extension provider: ${provider.getExtensionId()}")
}
```

---

## 4. Lifecycle and Initialization Flow

1. Plugin starts, project-level `ExtensionManager.initialize(configuredExtensionId?)` runs:
   - Registers all providers (including Cline)
   - If the user has configured a provider and it is available, set it as the current provider; otherwise, do not set a default
2. The UI layer refreshes buttons/menus based on the current provider (`DynamicButtonManager` for buttons, `DynamicContextMenuManager` for menus)
3. When `initializeCurrentProvider()` is called, it delegates to `ClineExtensionProvider.initialize(project)` to complete extension configuration initialization
4. User triggers UI actions → execute command → host/frontend (WebView) synchronization

Note: When no provider is configured, the initialization flow does not automatically select a default provider. `setDefaultExtensionProvider()` remains an optional strategy and is disabled by default.

---

## 5. Availability Check and Extension File Layout

- `ClineExtensionProvider.isAvailable(project)` checks in order:
  - Project/user-space extension directory: `${VsixManager.getBaseDirectory()}/${config.codeDir}`
  - Plugin built-in resource directory: `PluginResourceUtil.getResourcePath(PLUGIN_ID, config.codeDir)`

```45:75:jetbrains_plugin/src/main/kotlin/com/sina/weibo/agent/extensions/plugin/cline/ClineExtensionProvider.kt
override fun isAvailable(project: Project): Boolean {
    // Check if roo-code extension files exist
    val extensionConfig = ExtensionConfiguration.getInstance(project)
    val config = extensionConfig.getConfig(ExtensionType.CLINE)

    // First check project paths
    val possiblePaths = listOf(
        "${getBaseDirectory()}/${config.codeDir}"
    )

    if (possiblePaths.any { File(it).exists() }) {
        return true
    }

    // Then check plugin resources (for built-in extensions)
    try {
        val pluginResourcePath = PluginResourceUtil.getResourcePath(
            PluginConstants.PLUGIN_ID,
            config.codeDir
        )
        if (pluginResourcePath != null && File(pluginResourcePath).exists()) {
            return true
        }
    } catch (e: Exception) {
        // Ignore exceptions when checking plugin resources
    }

    // For development/testing, always return true if we can't find the files
    // This allows the extension to work even without the actual extension files
    return false
}
```

Note: The comments and return value above are inconsistent. The current implementation strictly returns false when files are not found. If you need to relax the rule during development, you can temporarily return `true`, but this is not recommended for acceptance/release.

- Key metadata comes from `ExtensionConfiguration.getConfig(ExtensionType.CLINE)`, including:
  - `codeDir` (extension code directory)
  - `publisher`, `version`, `mainFile`, `activationEvents`, `engines`, `capabilities`, `extensionDependencies`

---

## 6. UI Integration (Buttons and Context Menu)

- The button set is returned by `ClineButtonProvider.getButtons(project)`:
  - New Task (Plus) → `cline.plusButtonClicked`
  - MCP → `cline.mcpButtonClicked`
  - History → `cline.historyButtonClicked`
  - Account → `cline.accountButtonClicked`
  - Settings → `cline.settingsButtonClicked`
- Display policy is controlled by `ClineButtonConfiguration` (this implementation shows Plus/Prompts/History/Settings and hides MCP/Marketplace)

```177:186:jetbrains_plugin/src/main/kotlin/com/sina/weibo/agent/extensions/plugin/cline/ClineButtonProvider.kt
private class ClineButtonConfiguration : ButtonConfiguration {
    override fun isButtonVisible(buttonType: ButtonType): Boolean {
        return when (buttonType) {
            ButtonType.PLUS,
            ButtonType.PROMPTS,
            ButtonType.HISTORY,
            ButtonType.SETTINGS -> true
            ButtonType.MCP,
            ButtonType.MARKETPLACE -> false
        }
    }
}
```

Note: `getButtons(...)` returns a set that includes the MCP button, but visibility is decided by `ClineButtonConfiguration`, which hides MCP/Marketplace by default.

- The context menu is provided by `ClineContextMenuProvider`, which currently only defines visibility rules and does not return concrete `AnAction` lists (placeholder). It still controls which actions are visible/displayed.

---

## 7. Commands and Event Flow

- Core commands (the “message” channel to interact with the host):
  - `cline.plusButtonClicked`
  - `cline.mcpButtonClicked`
  - `cline.historyButtonClicked`
  - `cline.accountButtonClicked`
  - `cline.settingsButtonClicked`
- In the “New Task” button, WebView availability is checked via `WebViewManager.getLatestWebView()` before executing the command; otherwise, a friendly warning is shown to avoid blank screens or no-op actions.

Command registration/dispatch entry is in the `com.sina.weibo.agent.actions` package, and `ClineButtonProvider` triggers execution via `executeCommand("cline.xxx", ...)`.

---

## 8. WebView Interaction Notes

- Obtain the WebView manager via `project.getService(WebViewManager::class.java)`.
- It is recommended to check WebView availability before any action that depends on frontend rendering.
- If unavailable, warn the user and guide initialization (e.g., open the panel/login first).
- When a `WebViewManager` instance is unavailable, the current implementation only logs a warning (no dialog). When the manager exists but there are no WebView instances, a “WebView Not Available” dialog is shown.

---

## 9. Extension Switching and Configuration Updates

- Switch the current extension provider with the project-level `extensions/core/ExtensionManager.setCurrentProvider(extensionId, forceRestart?)`:
  - Invokes the new provider’s `initialize(project)`
  - Updates the configuration center `ExtensionConfigurationManager.setCurrentExtensionId(extensionId)`
  - Refreshes dynamic configurations for buttons and context menus
  - Publishes extension change events (`ExtensionChangeListener`)
- If you need to forcibly restart related processes/UI, use `switchExtensionProvider(extensionId, forceRestart=true)` for a full switch procedure.

```163:171:jetbrains_plugin/src/main/kotlin/com/sina/weibo/agent/extensions/core/ExtensionManager.kt
fun setCurrentProvider(extensionId: String, forceRestart: Boolean? = false): Boolean {
    val provider = extensionProviders[extensionId]
    if (provider != null && provider.isAvailable(project)) {
        val oldProvider = currentProvider
        if (forceRestart == false) {
            currentProvider = provider
        }

        // Initialize new provider (but don't restart the process)
        provider.initialize(project)
```

---

## 10. Practical Steps to Integrate Cline from Scratch

1. **Implement Provider**
   - Implement `ClineExtensionProvider` under `extensions/plugin/cline/`, ensure `getExtensionId()="cline"` and implement `initialize/isAvailable/getConfiguration/dispose`.
2. **Register Provider**
   - Ensure the project-level `extensions/core/ExtensionManager.getAllExtensions()` includes Cline:
     - Already built-in with `add(ClineExtensionProvider())`.
3. **UI Integration**
   - Define button set and their commands in `ClineButtonProvider`; check WebView availability before critical actions if needed.
   - Declare context menu visibility policies in `ClineContextMenuProvider`; optionally add `getContextMenuActions`.
4. **Command Dispatch and Host Wiring**
   - Ensure backend handlers are registered for `cline.*` commands (typically in a command dispatch center/message bus), and the frontend WebView can receive and render.
5. **Prepare Extension Files**
   - Place extension files (including `package.json` and the `main` entry) under `${VsixManager.getBaseDirectory()}/${config.codeDir}`, or bundle as plugin resources.
6. **Configuration and Metadata**
   - Verify `codeDir/publisher/version/mainFile/activationEvents/engines/capabilities/extensionDependencies` in `ExtensionConfiguration(ExtensionType.CLINE)` matches the actual files.
7. **Initialization and Switching**
   - After project startup, call global `ExtensionManager.initialize()`, and call `setCurrentProvider("cline")` to switch when necessary.
8. **Self-test**
   - Click toolbar buttons and context menu items to confirm command dispatch, WebView behavior, and reasonable logging.

---

## 11. Debugging and Logging

- All key flows have `Logger` records:
  - Button clicks, WebView availability, command success/failure logs with clear prefixes (including ✅/⚠️/❌)
- Recommended to observe in IDE “Event Log” and “Run/Debug Console”:
  - Verify whether `isAvailable` path checks matched
  - Verify whether `initialize` was invoked
  - Verify command dispatch and handling

---

## 12. Common Issues and Recommendations

- **Button clicks have no effect**: Often due to uninitialized WebView. Ensure the relevant panel is open or a WebView is available. The Cline “New Task” button already checks availability and shows user-friendly prompts.
- **`isAvailable=false`**: Check whether the combination of `config.codeDir` and `VsixManager.getBaseDirectory()` exists, or whether the plugin resource path is correct. Do not force returning `true` during development; it can mask deployment issues.
- **UI not refreshed after switching**: Ensure you called `setCurrentProvider(..., forceRestart=false)` so the dynamic managers refresh buttons/menus.
- **Commands not handled**: Ensure backend command dispatch registers handlers for `cline.*`, and the frontend WebView listens and responds.

---

## 13. Extension and Customization Suggestions

- Return concrete `AnAction` items in `ClineContextMenuProvider.getContextMenuActions(project)` to hook Cline’s context actions to commands for a more consistent UX.
- Dynamically control button visibility in `ClineButtonConfiguration` based on license/account state.
- In `ClineExtensionProvider.initialize`, optionally fetch remote configuration or perform auth initialization with proper error handling.

---

## 14. Bridging to the “Core Extension System” (Optional Deep Dive)

If you want to bridge to the lower-level VSCode compatibility layer (parse/register/activate `package.json`):

- Use the core `com.sina.weibo.agent.core.ExtensionManager` to:
  - Parse extension `package.json`
  - Register via `registerExtension(path, config)`
  - Activate via `activateExtension(extensionId, rpcProtocol)`
- This layer invokes the host `ExtHostExtensionService.activate(...)` via JSON-RPC/IPC, returns a `LazyPromise`, and ultimately converts to `CompletableFuture<Boolean>`.

```171:205:jetbrains_plugin/src/main/kotlin/com/sina/weibo/agent/core/ExtensionManager.kt
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
```

---

## 15. Acceptance Checklist (Self-Verification After Integration)

- **Provider**: `ClineExtensionProvider` exists and is registered by the global manager; `isAvailable` works correctly
- **UI**: Toolbar buttons are visible and usable; context menu policy is correct
- **Commands**: All `cline.*` commands are dispatched and handled
- **WebView**: Availability checks and user prompts function as expected
- **Configuration**: `ExtensionConfiguration(ExtensionType.CLINE)` matches the actual extension files


