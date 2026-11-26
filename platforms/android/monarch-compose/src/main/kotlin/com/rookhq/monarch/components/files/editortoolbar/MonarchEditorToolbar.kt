package com.rookhq.monarch.components.files.editortoolbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.actions.iconbutton.MonarchIconButton
import com.rookhq.monarch.components.datadisplay.tag.MonarchChip
import com.rookhq.monarch.components.overlays.menu.MonarchMenu
import com.rookhq.monarch.components.overlays.menu.MonarchMenuItem
import com.rookhq.monarch.internal.MonarchGlyphs
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme

/**
 * A formatting toolbar for a text editor: undo and redo, a text style menu,
 * inline formats, lists, quotes, code and links, plus product tools.
 *
 * Built by Monarch from Monarch controls in one row that scrolls
 * sideways when it is wider than the screen: formats are toggle `MonarchIconButton`s (or `MonarchChip`s when
 * the app gives no icon), undo, redo and product tools are `MonarchIconButton`s
 * (or ghost `MonarchButton`s), and neighbouring text styles are one
 * `MonarchMenu`. The toolbar is editor-agnostic: the app says which formats
 * are on at the caret and applies what is pressed. Place it above the
 * keyboard with `Modifier.imePadding()` at the bottom of the screen, or
 * above the editor.
 *
 * @param onAction runs when a tool is pressed or a text style is chosen. The app applies it to its editor.
 * @param modifier the modifier of the toolbar.
 * @param groups the tools in groups separated by dividers. Neighbouring paragraph and heading tools become one
 * text style menu; empty groups are dropped. [MonarchEditorTool.InlineGroups] gives inline formats only.
 * @param active the formats that are on at the caret or selection, and the current text style.
 * @param disabled tools that cannot be used now, such as undo with nothing to undo.
 * @param enabled whether any tool can be used, for example `false` while the editor loads.
 * @param icons the icon of each tool, drawn at the Material icon size in the control's content colour.
 * Monarch draws the web's glyph for each tool by default; a tool without a glyph shows its name.
 * @param labels replacements for the built-in strings.
 * @param textStyleMenu whether neighbouring text styles are one menu; by default each is a toggle button, as on
 * the web (T, H1, H2, H3).
 * @param onLink makes the link tool open a form for the address. Receives a safe address to apply, or `null`
 * when Remove link is pressed. Without it, the link tool goes to [onAction].
 * @param scrollable scrolls the tools sideways when they are wider than the toolbar. Turn it off when the toolbar
 * sits in a row that scrolls itself, such as the tools of a `MonarchMessageComposer`.
 * @param wrap wraps the groups onto a second line when they are wider than the toolbar, so every tool shows, as
 * the web toolbar does on a narrow screen. Takes the place of [scrollable].
 */
@OptIn(ExperimentalLayoutApi::class)
@Suppress("LongParameterList")
@Composable
public fun MonarchEditorToolbar(
    onAction: (MonarchEditorTool) -> Unit,
    modifier: Modifier = Modifier,
    groups: List<List<MonarchEditorTool>> = MonarchEditorTool.DefaultGroups,
    active: Set<MonarchEditorTool> = emptySet(),
    disabled: Set<MonarchEditorTool> = emptySet(),
    enabled: Boolean = true,
    icons: Map<MonarchEditorTool, Painter> = emptyMap(),
    labels: MonarchEditorToolbarLabels = MonarchEditorToolbarLabels(),
    textStyleMenu: Boolean = false,
    onLink: ((String?) -> Unit)? = null,
    scrollable: Boolean = true,
    wrap: Boolean = false,
) {
    val tokens = MonarchEditorToolbarTokens
    val colors = MonarchTheme.colors
    val name = monarchString(labels.toolbar, R.string.monarch_editortoolbar_toolbar)
    var linkForm by rememberSaveable { mutableStateOf(false) }
    val press: (MonarchEditorTool) -> Unit = { tool ->
        if (tool == MonarchEditorTool.Link && onLink != null) linkForm = true else onAction(tool)
    }
    val items = remember(groups) { tokens.items(groups) }
    val rowModifier = modifier
        .background(tokens.container(colors))
        .then(if (scrollable && !wrap) Modifier.horizontalScroll(rememberScrollState()) else Modifier)
        .padding(horizontal = tokens.HorizontalPadding, vertical = tokens.VerticalPadding)
        .semantics {
            isTraversalGroup = true
            contentDescription = name
        }
    val state = EditorToolbarState(active, disabled, enabled, icons, labels, textStyleMenu)
    val groupsContent: @Composable () -> Unit = {
        items.forEachIndexed { index, group ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(tokens.GroupGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (index > 0) {
                    Box(
                        Modifier
                            .width(tokens.DividerWidth)
                            .height(tokens.DividerHeight)
                            .background(tokens.divider(colors))
                            .clearAndSetSemantics { },
                    )
                }
                EditorToolbarGroup(group = group, state = state, onPress = press, onAction = onAction)
            }
        }
    }
    if (wrap) {
        FlowRow(
            modifier = rowModifier,
            horizontalArrangement = Arrangement.spacedBy(tokens.GroupGap),
            verticalArrangement = Arrangement.spacedBy(tokens.GroupGap),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) { groupsContent() }
    } else {
        Row(
            modifier = rowModifier,
            horizontalArrangement = Arrangement.spacedBy(tokens.GroupGap),
            verticalAlignment = Alignment.CenterVertically,
        ) { groupsContent() }
    }
    if (linkForm && onLink != null) {
        MonarchEditorToolbarLinkForm(
            linkActive = MonarchEditorTool.Link in active,
            labels = labels,
            onDone = { address ->
                linkForm = false
                onLink(address)
            },
            onDismiss = { linkForm = false },
        )
    }
}

/** What the controls of the toolbar show: the formats on, the tools off, their icons and names. */
private class EditorToolbarState(
    val active: Set<MonarchEditorTool>,
    val disabled: Set<MonarchEditorTool>,
    val enabled: Boolean,
    val icons: Map<MonarchEditorTool, Painter>,
    val labels: MonarchEditorToolbarLabels,
    val textStyleMenu: Boolean,
)

/** One group of the toolbar: its buttons, toggles and text style menu. */
@Composable
private fun EditorToolbarGroup(
    group: List<EditorToolbarItem>,
    state: EditorToolbarState,
    onPress: (MonarchEditorTool) -> Unit,
    onAction: (MonarchEditorTool) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(MonarchEditorToolbarTokens.Gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        group.forEach { item ->
            val tools = when (item) {
                is EditorToolbarItem.Single -> listOf(item.tool)
                is EditorToolbarItem.StyleMenu -> item.tools
            }
            if (item is EditorToolbarItem.StyleMenu && state.textStyleMenu) {
                EditorStyleMenu(
                    tools = tools,
                    active = state.active,
                    enabled = state.enabled && tools.any { it !in state.disabled },
                    disabled = state.disabled,
                    labels = state.labels,
                    onAction = onAction,
                )
            } else {
                tools.forEach { tool ->
                    EditorTool(
                        tool = tool,
                        checked = tool in state.active,
                        enabled = state.enabled && tool !in state.disabled,
                        given = state.icons[tool],
                        labels = state.labels,
                        onPress = onPress,
                    )
                }
            }
        }
    }
}

/** The glyph the web draws on a tool, drawn by default; `null` for a tool that shows its name. */
private fun builtInGlyph(tool: MonarchEditorTool): ImageVector? = when (tool) {
    MonarchEditorTool.Undo -> MonarchGlyphs.Undo
    MonarchEditorTool.Redo -> MonarchGlyphs.Redo
    MonarchEditorTool.Bold -> MonarchGlyphs.FormatBold
    MonarchEditorTool.Italic -> MonarchGlyphs.FormatItalic
    MonarchEditorTool.Strikethrough -> MonarchGlyphs.FormatStrikethrough
    MonarchEditorTool.Code -> MonarchGlyphs.Code
    MonarchEditorTool.Link -> MonarchGlyphs.Link
    MonarchEditorTool.BulletList -> MonarchGlyphs.FormatListBulleted
    MonarchEditorTool.OrderedList -> MonarchGlyphs.FormatListNumbered
    MonarchEditorTool.Blockquote -> MonarchGlyphs.FormatQuote
    MonarchEditorTool.CodeBlock -> MonarchGlyphs.DataObject
    else -> blockGlyph(tool)
}

/** The glyph of the task list, the paragraph and the headings. */
private fun blockGlyph(tool: MonarchEditorTool): ImageVector? = when (tool) {
    MonarchEditorTool.TaskList -> MonarchGlyphs.Checklist
    MonarchEditorTool.Paragraph -> MonarchGlyphs.Title
    else -> headingGlyph(tool.level)
}

/** The web's heading glyphs, level 1 first. */
private val HeadingGlyphs: List<ImageVector> by lazy {
    listOf(
        MonarchGlyphs.FormatH1,
        MonarchGlyphs.FormatH2,
        MonarchGlyphs.FormatH3,
        MonarchGlyphs.FormatH4,
        MonarchGlyphs.FormatH5,
        MonarchGlyphs.FormatH6,
    )
}

/** The web's heading glyph for [level], or `null` for a tool that is not a heading. */
private fun headingGlyph(level: Int?): ImageVector? = level?.let { HeadingGlyphs.getOrNull(it - 1) }

/** The spoken and visible name of a tool: the label, the custom title, the heading or the built-in name. */
@Composable
private fun editorToolName(tool: MonarchEditorTool, labels: MonarchEditorToolbarLabels): String {
    val level = tool.level
    val builtIn = MonarchEditorToolbarTokens.nameRes(tool)
    return labels.tools[tool] ?: tool.title ?: when {
        level != null -> stringResource(R.string.monarch_editortoolbar_heading, level)
        builtIn != null -> stringResource(builtIn)
        else -> tool.id
    }
}

@Suppress("LongParameterList")
@Composable
private fun EditorTool(
    tool: MonarchEditorTool,
    checked: Boolean,
    enabled: Boolean,
    given: Painter?,
    labels: MonarchEditorToolbarLabels,
    onPress: (MonarchEditorTool) -> Unit,
) {
    val builtIn = builtInGlyph(tool)?.let { rememberVectorPainter(it) }
    val icon = given ?: builtIn
    val name = editorToolName(tool, labels)
    val toggle = MonarchEditorToolbarTokens.isToggle(tool) || tool.kind == EditorToolbarToolKind.TextStyle
    when {
        toggle && icon != null -> MonarchIconButton(
            label = name,
            checked = checked,
            onCheckedChange = { onPress(tool) },
            enabled = enabled,
        ) { Icon(icon, contentDescription = null) }
        toggle -> MonarchChip(
            text = name,
            selected = checked,
            onSelectedChange = { onPress(tool) },
            enabled = enabled,
        )
        icon != null -> MonarchIconButton(label = name, onClick = { onPress(tool) }, enabled = enabled) {
            Icon(icon, contentDescription = null)
        }
        else -> MonarchButton(
            text = name,
            onClick = { onPress(tool) },
            variant = MonarchButtonVariant.Ghost,
            size = MonarchButtonSize.Sm,
            enabled = enabled,
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun EditorStyleMenu(
    tools: List<MonarchEditorTool>,
    active: Set<MonarchEditorTool>,
    enabled: Boolean,
    disabled: Set<MonarchEditorTool>,
    labels: MonarchEditorToolbarLabels,
    onAction: (MonarchEditorTool) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val menuName = monarchString(labels.textStyle, R.string.monarch_editortoolbar_text_style)
    val current = MonarchEditorToolbarTokens.currentStyle(tools, active)
    val currentName = current?.let { editorToolName(it, labels) }
    Box {
        MonarchButton(
            onClick = { expanded = true },
            modifier = Modifier.semantics {
                contentDescription = menuName
                if (currentName != null) stateDescription = currentName
            },
            variant = MonarchButtonVariant.Ghost,
            size = MonarchButtonSize.Sm,
            enabled = enabled,
        ) { Text(currentName ?: menuName) }
        MonarchMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
            tools.forEach { tool ->
                MonarchMenuItem(
                    text = editorToolName(tool, labels),
                    onClick = {
                        expanded = false
                        onAction(tool)
                    },
                    enabled = tool !in disabled,
                    selected = tool == current,
                )
            }
        }
    }
}
