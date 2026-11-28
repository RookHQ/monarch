package com.rookhq.monarch.components.files.editortoolbar

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.R
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** One control of the toolbar: a single tool, or neighbouring text styles as one menu. */
internal sealed interface EditorToolbarItem {
    /** A button or a toggle. */
    data class Single(val tool: MonarchEditorTool) : EditorToolbarItem

    /** The text style menu. */
    data class StyleMenu(val tools: List<MonarchEditorTool>) : EditorToolbarItem
}

/** Component tokens and rules of MonarchEditorToolbar, built from primitives and roles only. */
internal object MonarchEditorToolbarTokens {
    val Gap: Dp = MonarchTokens.Space.space1.dp
    val GroupGap: Dp = MonarchTokens.Space.space2.dp
    val HorizontalPadding: Dp = MonarchTokens.Space.space2.dp
    val VerticalPadding: Dp = MonarchTokens.Space.space1.dp
    val DividerHeight: Dp = MonarchTokens.Space.space6.dp
    val DividerWidth: Dp = MonarchTokens.Space.px.dp
    val FormGap: Dp = MonarchTokens.Space.space3.dp

    private val schemes = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:")
    private val email = Regex("^[^@\\s/:]+@[^@\\s/:]+\\.[^@\\s/:]+$")
    private val domain = Regex("^[^\\s/:@]+\\.[^\\s/:@]{2,}(:\\d+)?(/\\S*)?$")
    private val allowedSchemes = setOf("http", "https", "mailto")

    /** The controls of [groups]: empty groups dropped, neighbouring text styles merged into one menu. */
    fun items(groups: List<List<MonarchEditorTool>>): List<List<EditorToolbarItem>> =
        groups.filter { it.isNotEmpty() }.map { group ->
            val items = mutableListOf<EditorToolbarItem>()
            group.forEach { tool ->
                val last = items.lastOrNull()
                when {
                    tool.kind != EditorToolbarToolKind.TextStyle -> items += EditorToolbarItem.Single(tool)
                    last is EditorToolbarItem.StyleMenu -> items[items.lastIndex] = last.copy(tools = last.tools + tool)
                    else -> items += EditorToolbarItem.StyleMenu(listOf(tool))
                }
            }
            items
        }

    /** The active text style among [tools], or `null` when none is. */
    fun currentStyle(tools: List<MonarchEditorTool>, active: Set<MonarchEditorTool>): MonarchEditorTool? =
        tools.firstOrNull { it in active }

    /**
     * The address to apply for what was typed: http, https and mailto pass,
     * a bare domain becomes https, an email address becomes mailto, and
     * everything else (javascript, data, other schemes, spaces) is refused
     * with `null`.
     */
    fun safeLink(input: String): String? {
        val text = input.trim()
        return when {
            text.isEmpty() || text.any { it.isWhitespace() } -> null
            schemes.containsMatchIn(text) -> {
                val scheme = text.substringBefore(':').lowercase()
                val rest = text.substringAfter(':').removePrefix("//")
                text.takeIf { scheme in allowedSchemes && rest.isNotEmpty() }
            }
            email.matches(text) -> "mailto:$text"
            domain.matches(text) -> "https://$text"
            else -> null
        }
    }

    /** Whether the tool turns on and off. */
    fun isToggle(tool: MonarchEditorTool): Boolean = tool.kind == EditorToolbarToolKind.Toggle

    private val names: Map<MonarchEditorTool, Int> by lazy {
        mapOf(
            MonarchEditorTool.Undo to R.string.monarch_editortoolbar_undo,
            MonarchEditorTool.Redo to R.string.monarch_editortoolbar_redo,
            MonarchEditorTool.Paragraph to R.string.monarch_editortoolbar_paragraph,
            MonarchEditorTool.Bold to R.string.monarch_editortoolbar_bold,
            MonarchEditorTool.Italic to R.string.monarch_editortoolbar_italic,
            MonarchEditorTool.Strikethrough to R.string.monarch_editortoolbar_strikethrough,
            MonarchEditorTool.Code to R.string.monarch_editortoolbar_code,
            MonarchEditorTool.Link to R.string.monarch_editortoolbar_link,
            MonarchEditorTool.BulletList to R.string.monarch_editortoolbar_bullet_list,
            MonarchEditorTool.OrderedList to R.string.monarch_editortoolbar_ordered_list,
            MonarchEditorTool.TaskList to R.string.monarch_editortoolbar_task_list,
            MonarchEditorTool.Blockquote to R.string.monarch_editortoolbar_blockquote,
            MonarchEditorTool.CodeBlock to R.string.monarch_editortoolbar_code_block,
        )
    }

    /** The built-in name of a tool, or `null` for a heading (a format string) and a custom tool (its title). */
    @StringRes
    fun nameRes(tool: MonarchEditorTool): Int? = names[tool]

    /** The background of the toolbar. */
    fun container(colors: MonarchColors): Color = colors.bgSurface

    /** The group dividers. */
    fun divider(colors: MonarchColors): Color = colors.borderSubtle
}
