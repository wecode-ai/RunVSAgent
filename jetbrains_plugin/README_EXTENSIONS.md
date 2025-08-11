# RunVSAgent Extension System

## Overview

RunVSAgent now supports a modular extension system that allows you to use different AI coding assistants without being tied to a specific implementation. The system is completely decoupled, making it easy to add new extensions or switch between existing ones.

## Supported Extensions

### 1. Roo Code (Default)
- **ID**: `roo-code`
- **Description**: AI-powered code assistant with intelligent code generation
- **Publisher**: WeCode-AI
- **Directory**: `roo-code/`

## Quick Start

### For Users

1. **Select Extension via UI**:
   - Open your JetBrains IDE
   - Go to the RunVSAgent toolbar
   - Click the "Select Extension" button
   - Choose your preferred extension from the list

2. **Configure via File**:
   - Create a `.vscode-agent` file in your project root
   - Add the following content:
   ```properties
   extension.type=roo-code
   ```
   - Supported values: `roo-code`

3. **Extension Directory Structure**:
   ```
   your-project/
   ├── .vscode-agent          # Configuration file
   ├── roo-code/              # Roo Code extension files
   │   ├── package.json
   │   ├── dist/
   │   └── src/
   └── custom/                # Custom extension files
   ```

### For Developers

#### Adding a New Extension

1. **Create Extension Package**:
   ```bash
   mkdir -p src/main/kotlin/com/sina/weibo/agent/extensions/yourextension
   ```

2. **Implement ExtensionProvider**:
   ```kotlin
   // src/main/kotlin/com/sina/weibo/agent/extensions/yourextension/YourExtensionProvider.kt
   package com.sina.weibo.agent.extensions.yourextension
   
   import com.intellij.openapi.project.Project
   import com.sina.weibo.agent.extensions.ExtensionProvider
   
   class YourExtensionProvider : ExtensionProvider {
       override fun getExtensionId(): String = "your-extension"
       override fun getDisplayName(): String = "Your Extension"
       override fun getDescription(): String = "Your extension description"
       
       override fun initialize(project: Project) {
           // Initialize your extension
       }
       
       override fun isAvailable(project: Project): Boolean {
           val projectPath = project.basePath ?: return false
           return java.io.File("$projectPath/your-extension").exists()
       }
       
       override fun getConfiguration(project: Project): ExtensionConfiguration {
           return object : ExtensionConfiguration {
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

3. **Register Extension**:
   ```kotlin
   // In ExtensionManager.kt
   private fun registerExtensionProviders() {
       // ... existing providers ...
       
       // Register your extension provider
       val yourProvider = com.sina.weibo.agent.extensions.yourextension.YourExtensionProvider()
       registerExtensionProvider(yourProvider)
   }
   ```

4. **Create Extension Files**:
   ```
   your-extension/
   ├── package.json
   ├── dist/
   │   └── extension.js
   └── src/
       └── extension.ts
   ```

#### Extension Configuration

Each extension can have its own configuration:

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

## API Reference

### ExtensionProvider Interface

```kotlin
interface ExtensionProvider {
    fun getExtensionId(): String
    fun getDisplayName(): String
    fun getDescription(): String
    fun initialize(project: Project)
    fun isAvailable(project: Project): Boolean
    fun getConfiguration(project: Project): ExtensionConfiguration
    fun dispose()
}
```

### ExtensionConfiguration Interface

```kotlin
interface ExtensionConfiguration {
    fun getCodeDir(): String
    fun getPublisher(): String
    fun getVersion(): String
    fun getMainFile(): String
    fun getActivationEvents(): List<String>
    fun getEngines(): Map<String, String>
    fun getCapabilities(): Map<String, Any>
    fun getExtensionDependencies(): List<String>
}
```

### ExtensionManager

```kotlin
// Get extension manager instance
val extensionManager = ExtensionManager.getInstance(project)

// Get current provider
val currentProvider = extensionManager.getCurrentProvider()

// Switch to different extension
extensionManager.setCurrentProvider("xxxx")

// Get all available providers
val availableProviders = extensionManager.getAvailableProviders()

// Get specific provider
val rooProvider = extensionManager.getProvider("roo-code")
```

## Configuration Options

### .vscode-agent File

```properties
# Extension type to use
extension.type=roo-code

# Debug mode
debug.mode=idea
debug.resource=/path/to/debug/resources

# Extension-specific settings
roo.debug.enabled=false
roo.api.endpoint=https://api.roo-code.com

copilot.auth.token=your_github_token
copilot.auto_suggest=true

claude.api.key=your_anthropic_api_key
claude.model=claude-3-sonnet-20240229
```

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

## Testing

Run the extension tests:

```bash
./gradlew test --tests "com.sina.weibo.agent.extensions.ExtensionDecouplingTest"
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Implement your extension
4. Add tests
5. Submit a pull request

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details. 