// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.extensions.plugin.cline

import com.google.gson.Gson
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.sina.weibo.agent.actions.SupportPrompt
import com.sina.weibo.agent.actions.executeCommand
import com.sina.weibo.agent.extensions.ui.contextmenu.ExtensionContextMenuProvider
import com.sina.weibo.agent.extensions.ui.contextmenu.ContextMenuConfiguration
import com.sina.weibo.agent.extensions.ui.contextmenu.ContextMenuActionType
import com.sina.weibo.agent.webview.WebViewManager

/**
 * Cline extension context menu provider.
 * Provides context menu actions specific to Cline AI extension.
 * This includes Cline-specific functionality and commands.
 */
class ClineContextMenuProvider : ExtensionContextMenuProvider {
    
    override fun getExtensionId(): String = "cline"
    
    override fun getDisplayName(): String = "Cline AI"
    
    override fun getDescription(): String = "AI-powered coding assistant with Cline-specific context menu"
    
    override fun isAvailable(project: Project): Boolean {
        // Check if cline extension is available
        return true
    }
    
    override fun getContextMenuActions(project: Project): List<AnAction> {
        return listOf(
//            ClineExplainCodeAction(),
//            ClineFixCodeAction(),
//            ClineImproveCodeAction(),
//            ClineAddToContextAction(),
//            ClineNewTaskAction()
        )
    }
    
    override fun getContextMenuConfiguration(): ContextMenuConfiguration {
        return ClineContextMenuConfiguration()
    }
    
    /**
     * Cline context menu configuration - shows core actions only.
     */
    private class ClineContextMenuConfiguration : ContextMenuConfiguration {
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
        
        override fun getVisibleActions(): List<ContextMenuActionType> {
            return listOf(
                ContextMenuActionType.EXPLAIN_CODE,
                ContextMenuActionType.FIX_CODE,
                ContextMenuActionType.IMPROVE_CODE,
                ContextMenuActionType.ADD_TO_CONTEXT,
                ContextMenuActionType.NEW_TASK
            )
        }
    }

    /**
     * Cline action to explain selected code.
     * Uses Cline-specific command and prompt format.
     */
    class ClineExplainCodeAction : AnAction("Explain Code (Cline)") {
        private val logger: Logger = Logger.getInstance(ClineExplainCodeAction::class.java)
        
        override fun actionPerformed(e: AnActionEvent) {
            val project = e.project ?: return
            val editor = e.getData(CommonDataKeys.EDITOR) ?: return
            val file = e.dataContext.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
            
            val effectiveRange = ClineContextMenuProvider.getEffectiveRange(editor)
            if (effectiveRange == null) return
            
            val args = mutableMapOf<String, Any?>()
            args["filePath"] = file.path
            args["selectedText"] = effectiveRange.text
            args["startLine"] = effectiveRange.startLine + 1
            args["endLine"] = effectiveRange.endLine + 1

            executeCommand("cline.explainCode", project, args)
        }
    }

    /**
     * Cline action to fix code issues.
     * Uses Cline-specific command and prompt format.
     */
    class ClineFixCodeAction : AnAction("Fix Code (Cline)") {
        private val logger: Logger = Logger.getInstance(ClineFixCodeAction::class.java)
        
        override fun actionPerformed(e: AnActionEvent) {
            val project = e.project ?: return
            val editor = e.getData(CommonDataKeys.EDITOR) ?: return
            val file = e.dataContext.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
            
            val effectiveRange = ClineContextMenuProvider.getEffectiveRange(editor)
            if (effectiveRange == null) return
            
            val args = mutableMapOf<String, Any?>()
            args["filePath"] = file.path
            args["selectedText"] = effectiveRange.text
            args["startLine"] = effectiveRange.startLine + 1
            args["endLine"] = effectiveRange.endLine + 1
            executeCommand("cline.fixCode", project, args)
        }
    }

    /**
     * Cline action to improve code quality.
     * Uses Cline-specific command and prompt format.
     */
    class ClineImproveCodeAction : AnAction("Improve Code (Cline)") {
        private val logger: Logger = Logger.getInstance(ClineImproveCodeAction::class.java)
        
        override fun actionPerformed(e: AnActionEvent) {
            val project = e.project ?: return
            val editor = e.getData(CommonDataKeys.EDITOR) ?: return
            val file = e.dataContext.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
            
            val effectiveRange = ClineContextMenuProvider.getEffectiveRange(editor)
            if (effectiveRange == null) return
            
            val args = mutableMapOf<String, Any?>()
            args["filePath"] = file.path
            args["selectedText"] = effectiveRange.text
            args["startLine"] = effectiveRange.startLine + 1
            args["endLine"] = effectiveRange.endLine + 1
            executeCommand("cline.improveCode", project, args)
            
        }
    }

    /**
     * Cline action to add selected code to context.
     * Uses Cline-specific command and prompt format.
     */
    class ClineAddToContextAction : AnAction("Add to Context (Cline)") {
        private val logger: Logger = Logger.getInstance(ClineAddToContextAction::class.java)
        
        override fun actionPerformed(e: AnActionEvent) {
            val project = e.project ?: return
            val editor = e.getData(CommonDataKeys.EDITOR) ?: return
            val file = e.dataContext.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
            
            val effectiveRange = ClineContextMenuProvider.getEffectiveRange(editor)
            if (effectiveRange == null) return

            val range = ClineContextMenuProvider.Range(
                startLine = effectiveRange.startLine,
                startCharacter = effectiveRange.startCharacter,
                endLine = effectiveRange.endLine,
                endCharacter = effectiveRange.endCharacter
            )
            val rangeMap = mapOf(
                "startLine" to effectiveRange.startLine,
                "startCharacter" to effectiveRange.startCharacter,
                "endLine" to effectiveRange.endLine,
                "endCharacter" to effectiveRange.endCharacter
            )

            executeCommand("cline.addToChat", project, rangeMap)
        }
    }

    /**
     * Cline action to create a new task.
     * Uses Cline-specific command and prompt format.
     */
    class ClineNewTaskAction : AnAction("New Task (Cline)") {
        private val logger: Logger = Logger.getInstance(ClineNewTaskAction::class.java)
        
        override fun actionPerformed(e: AnActionEvent) {
            val project = e.project ?: return
            val editor = e.getData(CommonDataKeys.EDITOR) ?: return
            val file = e.dataContext.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
            
            val effectiveRange = ClineContextMenuProvider.getEffectiveRange(editor)
            if (effectiveRange == null) return
            
            val args = mutableMapOf<String, Any?>()
            args["filePath"] = file.path
            args["selectedText"] = effectiveRange.text
            args["startLine"] = effectiveRange.startLine + 1
            args["endLine"] = effectiveRange.endLine + 1
            
            ClineContextMenuProvider.handleClineCodeAction("cline.newTask", "NEW_TASK", args, project)
        }
    }

    /**
     * Data class representing an effective range of selected text.
     * Contains the selected text and its start/end line numbers.
     *
     * @property text The selected text content
     * @property startLine The starting line number (0-based)
     * @property endLine The ending line number (0-based)
     */
    data class EffectiveRange(
        val text: String,
        val startLine: Int,
        val endLine: Int,
        val startCharacter: Int,
        val endCharacter: Int,
    )

    companion object {
        /**
         * Gets the effective range and text from the current editor selection.
         *
         * @param editor The current editor instance
         * @return EffectiveRange object containing selected text and line numbers, or null if no selection
         */
        fun getEffectiveRange(editor: com.intellij.openapi.editor.Editor): EffectiveRange? {
            val document = editor.document
            val selectionModel = editor.selectionModel

            return if (selectionModel.hasSelection()) {
                val selectedText = selectionModel.selectedText ?: ""
                val startLine = document.getLineNumber(selectionModel.selectionStart)
                val endLine = document.getLineNumber(selectionModel.selectionEnd)
                val startCharacter = selectionModel.selectionStart - document.getLineStartOffset(startLine)
                val endCharacter = selectionModel.selectionEnd - document.getLineStartOffset(endLine)
                EffectiveRange(selectedText, startLine, endLine, startCharacter, endCharacter)
            } else {
                null
            }
        }

        /**
         * Core logic for handling Cline code actions.
         * Processes different types of commands and sends appropriate messages to the webview.
         * Uses Cline-specific command format and prompt templates.
         *
         * @param command The Cline command identifier
         * @param promptType The type of prompt to use
         * @param params Parameters for the action
         * @param project The current project
         */
        fun handleClineCodeAction(command: String, promptType: String, params: Map<String, Any?>, project: Project?) {
            val latestWebView = project?.getService(WebViewManager::class.java)?.getLatestWebView()
            if (latestWebView == null) {
                return
            }

            // Create message content based on command type
            val messageContent = when {
                // Add to context command
                command.contains("addToContext") -> {
                    mapOf(
                        "type" to "invoke",
                        "invoke" to "setChatBoxMessage",
                        "text" to createClinePrompt("ADD_TO_CONTEXT", params)
                    )
                }
                // Command executed in new task
                else -> {
                    val promptParams = params
                    val basePromptType = when {
                        command.contains("explain") -> "EXPLAIN"
                        command.contains("fix") -> "FIX"
                        command.contains("improve") -> "IMPROVE"
                        else -> promptType
                    }
                    mapOf(
                        "type" to "invoke",
                        "invoke" to "sendMessage",
                        "text" to SupportPrompt.create(basePromptType, promptParams)
                    )
                }
            }

            // Convert to JSON and send
            val messageJson = com.google.gson.Gson().toJson(messageContent)
            latestWebView.postMessageToWebView(messageJson)
        }

        /**
         * Creates a Cline-specific prompt by replacing placeholders in a template with actual values.
         *
         * @param promptType The type of prompt to create
         * @param params Parameters to substitute into the template
         * @return The final prompt with all placeholders replaced
         */
        fun createClinePrompt(promptType: String, params: Map<String, Any?>): String {
            val template = getClinePromptTemplate(promptType)
            return replacePlaceholders(template, params)
        }

        /**
         * Gets the Cline-specific template for a specific prompt type.
         * These templates are optimized for Cline AI's capabilities and style.
         *
         * @param type The type of prompt to retrieve
         * @return The template string for the specified prompt type
         */
        fun getClinePromptTemplate(type: String): String {
            return when (type) {
                "EXPLAIN" -> """Please explain this code from ${'$'}{filePath} (lines ${'$'}{startLine}-${'$'}{endLine}):

```${'$'}{selectedText}```

Focus on:
- What the code does and its purpose
- Key components and their relationships
- Important patterns or techniques used
- Any potential improvements or considerations"""
                
                "FIX" -> """Please fix any issues in this code from ${'$'}{filePath} (lines ${'$'}{startLine}-${'$'}{endLine}):

```${'$'}{selectedText}```

Please:
- Identify and fix any bugs or issues
- Improve error handling and edge cases
- Provide the corrected code
- Explain what was fixed and why"""
                
                "IMPROVE" -> """Please improve this code from ${'$'}{filePath} (lines ${'$'}{startLine}-${'$'}{endLine}):

```${'$'}{selectedText}```

Focus on improvements for:
- Code readability and maintainability
- Performance optimization
- Best practices and modern patterns
- Error handling and robustness

Provide the improved code with explanations for each enhancement."""
                
                "ADD_TO_CONTEXT" -> """Code from ${'$'}{filePath} (lines ${'$'}{startLine}-${'$'}{endLine}):
```${'$'}{selectedText}```"""
                
                "NEW_TASK" -> """${'$'}{selectedText}"""
                
                else -> ""
            }
        }

        /**
         * Replaces placeholders in a template with actual values.
         *
         * @param template The prompt template with placeholders
         * @param params Map of parameter values to replace placeholders
         * @return The processed prompt with placeholders replaced by actual values
         */
        fun replacePlaceholders(template: String, params: Map<String, Any?>): String {
            val pattern = Regex("""\$\{(.*?)}""")
            return pattern.replace(template) { matchResult ->
                val key = matchResult.groupValues[1]
                params[key]?.toString() ?: ""
            }
        }
    }

    /**
     * 与 VS Code 完全一致的 Position。
     */
    data class Position(val line: Int, val character: Int) : Comparable<Position> {

        init {
            require(line >= 0) { "line must be non-negative" }
            require(character >= 0) { "character must be non-negative" }
        }

        override fun compareTo(other: Position): Int {
            return when {
                line != other.line -> line - other.line
                else -> character - other.character
            }
        }

        fun isBefore(other: Position): Boolean = compareTo(other) < 0
        fun isBeforeOrEqual(other: Position): Boolean = compareTo(other) <= 0
        fun isAfter(other: Position): Boolean = compareTo(other) > 0
        fun isAfterOrEqual(other: Position): Boolean = compareTo(other) >= 0

        fun translate(lineDelta: Int = 0, characterDelta: Int = 0): Position {
            return Position(line + lineDelta, character + characterDelta)
        }

        fun translate(change: Position): Position {
            return Position(line + change.line, character + change.character)
        }

        fun with(line: Int = this.line, character: Int = this.character): Position {
            return Position(line, character)
        }

        override fun toString(): String = "($line,$character)"
    }

    /**
     * 与 VS Code 完全一致的 Range。
     */
    class Range(startLine: Int, startCharacter: Int, endLine: Int, endCharacter: Int) {

        val start: Position
        val end: Position

        constructor(start: Position, end: Position) :
                this(start.line, start.character, end.line, end.character)

        init {
            val s = Position(startLine, startCharacter)
            val e = Position(endLine, endCharacter)
            if (s <= e) {
                start = s
                end = e
            } else {
                start = e
                end = s
            }
        }

        val isEmpty: Boolean get() = start == end
        val isSingleLine: Boolean get() = start.line == end.line

        fun contains(position: Position): Boolean {
            return position >= start && position <= end
        }

        fun contains(range: Range): Boolean {
            return range.start >= start && range.end <= end
        }

        fun intersection(range: Range): Range? {
            val newStart = if (start >= range.start) start else range.start
            val newEnd = if (end <= range.end) end else range.end
            return if (newStart <= newEnd) Range(newStart, newEnd) else null
        }

        fun union(range: Range): Range {
            val newStart = if (start <= range.start) start else range.start
            val newEnd = if (end >= range.end) end else range.end
            return Range(newStart, newEnd)
        }

        fun with(start: Position = this.start, end: Position = this.end): Range {
            return Range(start, end)
        }

        override fun equals(other: Any?): Boolean {
            return other is Range && other.start == start && other.end == end
        }

        override fun hashCode(): Int {
            return 31 * start.hashCode() + end.hashCode()
        }

        override fun toString(): String {
            return "[${start.toString()}~${end.toString()}]"
        }
    }
}
