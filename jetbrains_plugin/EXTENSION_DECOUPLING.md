# Extension Decoupling System

## Overview

The RunVSAgent plugin has been refactored to support multiple extension types through a decoupled architecture. This allows the plugin to work with different AI coding assistants and extensions without being tied to a specific implementation.

## Architecture

### Core Components

1. **ExtensionProvider** - Interface that all extension implementations must implement
2. **ExtensionManager** - Global manager for all extension providers
3. **ExtensionConfiguration** - Interface for extension configuration
4. **ExtensionSelector** - UI component for selecting extensions

### Package Structure

```
src/main/kotlin/com/sina/weibo/agent/
├── extensions/                    # Extension system
│   ├── ExtensionProvider.kt      # Extension provider interface
│   ├── ExtensionManager.kt       # Global extension manager
│   ├── ExtensionConfiguration.kt # Extension configuration interface
│   ├── ui/                       # Extension UI components
│   │   └── ExtensionSelector.kt  # Extension selection dialog
│   ├── actions/                  # Extension actions
│   │   └── ExtensionSelectorAction.kt # Extension selection action
│   ├── roo/                      # Roo Code extension implementation
│   │   ├── ExtensionType.kt      # Roo extension types
│   │   ├── ExtensionConfiguration.kt # Roo extension configuration
│   │   ├── ExtensionManagerFactory.kt # Roo extension manager factory
│   │   ├── RooExtensionProvider.kt # Roo extension provider implementation
│   │   ├── ui/                   # Roo-specific UI components
│   │   │   └── ExtensionTypeSelector.kt
│   │   └── actions/              # Roo-specific actions
│   │       └── ExtensionTypeSelectorAction.kt
│   └── copilot/                  # Copilot extension implementation
│       ├── CopilotExtensionProvider.kt # Copilot extension provider
│       └── CopilotExtensionConfiguration.kt # Copilot configuration
```

### Extension Implementations

- **Roo Code** (`extensions.roo`) - AI-powered code assistant (default)
- **GitHub Copilot** (`extensions.copilot`) - AI pair programming assistant
- **Claude** (`extensions.claude`) - Anthropic's AI assistant (planned)
- **Custom Extensions** - Support for custom AI extensions

## How to Add a New Extension

### Step 1: Create Extension Package

Create a new package for your extension under `extensions/`:

```kotlin
// src/main/kotlin/com/sina/weibo/agent/extensions/yourextension/
package com.sina.weibo.agent.extensions.yourextension

import com.intellij.openapi.project.Project
import com.sina.weibo.agent.extensions.ExtensionProvider
import com.sina.weibo.agent.extensions.ExtensionConfiguration as BaseExtensionConfiguration

class YourExtensionProvider : ExtensionProvider {
    override fun getExtensionId(): String = "your-extension"
    override fun getDisplayName(): String = "Your Extension"
    override fun getDescription(): String = "Your extension description"
    
    override fun initialize(project: Project) {
        // Initialize your extension
    }
    
    override fun isAvailable(project: Project): Boolean {
        // Check if your extension is available
        val projectPath = project.basePath ?: return false
        return java.io.File("$projectPath/your-extension").exists()
    }
    
    override fun getConfiguration(project: Project): BaseExtensionConfiguration {
        return object : BaseExtensionConfiguration {
            override fun getCodeDir(): String = "your-extension"
            override fun getPublisher(): String = "YourPublisher"
            override fun getVersion(): String = "1.0.0"
            override fun getMainFile(): String = "./dist/extension.js"
            override fun getActivationEvents(): List<String> = listOf("onStartupFinished")
            override fun getEngines(): Map<String, String> = mapOf("vscode" to "^1.0.0")
            override fun getCapabilities(): Map<String, Any> = emptyMap()
            override fun getExtensionDependencies(): List<String> = emptyList()
        }
    }
    
    override fun dispose() {
        // Cleanup resources
    }
}
```

### Step 2: Create Extension Configuration

```kotlin
@Service(Service.Level.PROJECT)
class YourExtensionConfiguration(private val project: Project) {
    companion object {
        fun getInstance(project: Project): YourExtensionConfiguration {
            return project.getService(YourExtensionConfiguration::class.java)
                ?: error("YourExtensionConfiguration not found")
        }
    }
    
    fun initialize() {
        // Initialize configuration
    }
    
    fun getCurrentConfig(): YourExtensionConfig {
        return YourExtensionConfig.getDefault()
    }
}

data class YourExtensionConfig(
    val codeDir: String,
    val displayName: String,
    val description: String,
    val publisher: String,
    val version: String,
    val mainFile: String,
    val activationEvents: List<String>,
    val engines: Map<String, String>,
    val capabilities: Map<String, Any>,
    val extensionDependencies: List<String>
) {
    companion object {
        fun getDefault(): YourExtensionConfig {
            return YourExtensionConfig(
                codeDir = "your-extension",
                displayName = "Your Extension",
                description = "Your extension description",
                publisher = "YourPublisher",
                version = "1.0.0",
                mainFile = "./dist/extension.js",
                activationEvents = listOf("onStartupFinished"),
                engines = mapOf("vscode" to "^1.0.0"),
                capabilities = emptyMap(),
                extensionDependencies = emptyList()
            )
        }
    }
}
```

### Step 3: Register Extension Provider

Add your extension provider to the `ExtensionManager`:

```kotlin
// In ExtensionManager.kt
private fun registerExtensionProviders() {
    // Register Roo Code extension provider
    val rooProvider = com.sina.weibo.agent.extensions.roo.RooExtensionProvider()
    registerExtensionProvider(rooProvider)
    
    // Register your extension provider
    val yourProvider = com.sina.weibo.agent.extensions.yourextension.YourExtensionProvider()
    registerExtensionProvider(yourProvider)
}
```

### Step 4: Extension Directory Structure

Your extension should follow this directory structure:

```
your-extension/
├── package.json          # Extension manifest
├── dist/
│   └── extension.js      # Main extension file
├── src/
│   └── extension.ts      # TypeScript source
└── README.md             # Extension documentation
```

Example `package.json`:

```json
{
  "name": "your-extension",
  "displayName": "Your Extension",
  "description": "Your extension description",
  "version": "1.0.0",
  "publisher": "YourPublisher",
  "engines": {
    "vscode": "^1.0.0"
  },
  "activationEvents": [
    "onStartupFinished"
  ],
  "main": "./dist/extension.js"
}
```

## Usage

### Extension Selection

Users can select extensions through:

1. **UI Action**: Use the "Select Extension" action in the toolbar
2. **Configuration File**: Set `extension.type` in `.vscode-agent` file
3. **Programmatic**: Use `ExtensionManager.setCurrentProvider()`

### Configuration File

Create a `.vscode-agent` file in your project root:

```properties
# Extension type to use
# Supported values: roo-code
extension.type=roo-code

# Additional configuration options
debug.mode=idea
debug.resource=/path/to/debug/resources
```

### Programmatic Usage

```kotlin
// Get extension manager
val extensionManager = ExtensionManager.getInstance(project)

// Get current provider
val currentProvider = extensionManager.getCurrentProvider()

// Switch to different extension
extensionManager.setCurrentProvider("roo-code")

// Get all available providers
val availableProviders = extensionManager.getAvailableProviders()
```

## Migration from Hardcoded roo-code

### What Changed

1. **Package Structure** - All roo-code related code moved to `extensions.roo` package
2. **Extension Interface** - New `ExtensionProvider` interface for all extensions
3. **Global Manager** - `ExtensionManager` manages all extension providers
4. **UI Components** - Generic extension selector with provider-specific UI

### Migration Steps

1. **Update imports**:
   ```kotlin
   // Old way
   import com.sina.weibo.agent.core.ExtensionConfiguration
   
   // New way
   import com.sina.weibo.agent.extensions.ExtensionManager
   ```

2. **Use extension manager**:
   ```kotlin
   // Old way
   val config = ExtensionConfiguration.getInstance(project)
   
   // New way
   val extensionManager = ExtensionManager.getInstance(project)
   val provider = extensionManager.getCurrentProvider()
   val config = provider?.getConfiguration(project)
   ```

## Benefits

1. **Complete Separation** - Each extension is completely independent
2. **Modular Architecture** - Easy to add/remove extensions
3. **Clean Interfaces** - Well-defined contracts between components
4. **User Choice** - Users can select preferred extensions
5. **Maintainability** - Each extension can be maintained separately

## Future Enhancements

1. **Dynamic Extension Loading** - Load extensions at runtime
2. **Extension Marketplace** - Browse and install new extensions
3. **Extension Validation** - Validate extension compatibility
4. **Performance Optimization** - Lazy loading of unused extensions
5. **Extension Dependencies** - Handle extension dependencies

## Troubleshooting

### Extension Not Found

1. Check if extension directory exists in expected locations
2. Verify extension provider is registered in `ExtensionManager`
3. Check extension configuration is correct

### Extension Not Loading

1. Verify `package.json` is valid
2. Check main file path is correct
3. Ensure extension files are accessible

### Configuration Issues

1. Check `.vscode-agent` file format
2. Verify extension type is supported
3. Ensure configuration values are correct 