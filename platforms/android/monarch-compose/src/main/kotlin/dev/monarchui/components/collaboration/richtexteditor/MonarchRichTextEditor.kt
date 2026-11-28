package dev.monarchui.components.collaboration.richtexteditor

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.R
import dev.monarchui.accessibility.monarchFocusRing
import dev.monarchui.components.layout.divider.MonarchDivider
import dev.monarchui.components.layout.divider.MonarchDividerVariant
import dev.monarchui.components.navigation.link.MonarchLinkDefaults
import dev.monarchui.components.collaboration.mentionpicker.MonarchMentionItem
import dev.monarchui.components.collaboration.mentionpicker.MonarchMentionPicker
import dev.monarchui.components.collaboration.mentionpicker.MonarchMentionPickerStatus
import dev.monarchui.components.collaboration.mentionpicker.MonarchMentionQuery
import dev.monarchui.components.files.editortoolbar.MonarchEditorTool
import dev.monarchui.components.files.editortoolbar.MonarchEditorToolbar
import dev.monarchui.internal.monarchSurface
import dev.monarchui.theme.MonarchTheme
import java.text.NumberFormat

/**
 * A rich-text editor: a formatting toolbar for bold, italic, strikethrough,
 * code, links, headings and lists above the text, a prompt, a character
 * count against a limit, and @mention suggestions.
 *
 * Compose has no rich-text editor, so Monarch builds one on the Compose
 * `BasicTextField` with a `TextFieldValue`: the document is plain text plus
 * [MonarchRichTextSpan]s in a [MonarchRichTextEditorState], which keeps the
 * spans in step with every edit and draws them with a `VisualTransformation`.
 * The toolbar is `MonarchEditorToolbar`, the suggestions `MonarchMentionPicker`.
 * Lists are text prefixes ("• ", "1. "): Return continues a list and Return on
 * an empty item ends it. Typing at the end of a format continues it; a format
 * turned on or off at the caret applies to what is typed next.
 *
 * Experimental until the document interchange format is agreed across
 * platforms: no HTML, Markdown or JSON conversion is shipped; store [MonarchRichTextEditorState.text]
 * and [MonarchRichTextEditorState.spans].
 *
 * @param label the spoken name of the text area, already localized. Show a visible label nearby.
 * @param state the document, its selection and its undo history.
 * @param modifier the modifier of the editor.
 * @param prompt shown while the document is empty.
 * @param tools the toolbar's tools in groups; an empty list for no toolbar. Undo, redo, body text, headings 1
 * to 3, bold, italic, strikethrough, code, link and both lists are applied by the editor; custom tools go to
 * [onTool]; task lists, quotes, code blocks and headings 4 to 6 are left out.
 * @param maxLength the most characters. Typing and pasting stop there; a longer document that is set shows
 * the count and border in the danger colour.
 * @param showCharacterCount shows the count. By default, shown when there is a [maxLength].
 * @param enabled whether the text and the toolbar can be used.
 * @param readOnly shows the document as text, without the toolbar or the box; links open on a tap.
 * @param invalid draws the danger border, for a validation error shown nearby.
 * @param mentionItems the suggestions for the mention being typed after @ or #; `null` turns mentions off.
 * @param mentionStatus the state of the mention search.
 * @param icons the icons of the toolbar's tools. Monarch bundles no icons: a tool without one shows its name.
 * @param labels replacements for the built-in strings.
 * @param onMentionSearch receives the mention being typed, or `null` when there is none; search and update
 * [mentionItems]. Also called by the suggestions' retry.
 * @param onMention receives the chosen person or channel after "@Name " is written into the text.
 * @param onTool receives the custom tools of the toolbar.
 * @param interactionSource the interaction source of the text field.
 */
@ExperimentalMonarchApi
@Suppress("LongParameterList", "LongMethod", "CyclomaticComplexMethod")
@Composable
public fun MonarchRichTextEditor(
    label: String,
    state: MonarchRichTextEditorState,
    modifier: Modifier = Modifier,
    prompt: String? = null,
    tools: List<List<MonarchEditorTool>> = MonarchRichTextEditorDefaults.Tools,
    maxLength: Int? = null,
    showCharacterCount: Boolean? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    invalid: Boolean = false,
    mentionItems: List<MonarchMentionItem>? = null,
    mentionStatus: MonarchMentionPickerStatus = MonarchMentionPickerStatus.Ready,
    icons: Map<MonarchEditorTool, Painter> = emptyMap(),
    labels: MonarchRichTextEditorLabels = MonarchRichTextEditorLabels(),
    onMentionSearch: ((MonarchMentionQuery?) -> Unit)? = null,
    onMention: ((MonarchMentionItem) -> Unit)? = null,
    onTool: ((MonarchEditorTool) -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val tokens = MonarchRichTextEditorTokens
    val colors = MonarchTheme.colors
    val typography = MonarchTheme.typography
    if (readOnly) {
        val links = MonarchLinkDefaults.styles()
        Text(
            text = tokens.annotated(state.text, state.spans, typography, colors, links),
            modifier = modifier,
            style = typography.body,
            color = colors.textDefault,
        )
        return
    }
    val over = MonarchRichTextEditorDefaults.isOverLimit(state.text, maxLength)
    val overText = if (over && maxLength != null) richTextOver(state.text.length - maxLength, labels) else null
    val error = invalid || over
    val box = tokens.box(colors, enabled, error)
    val shape = MonarchTheme.shapes.md
    val groups = remember(tools) { tools.map { it.filter(::richTextHandles) }.filter { it.isNotEmpty() } }
    val spans = state.spans
    val transformation = remember(spans, typography, colors) {
        VisualTransformation { text ->
            TransformedText(tokens.annotated(text.text, spans, typography, colors), OffsetMapping.Identity)
        }
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(tokens.Gap)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .monarchFocusRing(shape)
                .monarchSurface(box, shape)
                .clip(shape),
        ) {
            if (groups.isNotEmpty()) {
                MonarchEditorToolbar(
                    onAction = { tool ->
                        if (tool.id.startsWith(CUSTOM)) onTool?.invoke(tool) else state.applyTool(tool)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    groups = groups,
                    active = state.activeTools(),
                    disabled = buildSet {
                        if (!state.canUndo) add(MonarchEditorTool.Undo)
                        if (!state.canRedo) add(MonarchEditorTool.Redo)
                    },
                    enabled = enabled,
                    icons = icons,
                    labels = labels.toolbar,
                    onLink = { state.applyLink(it) },
                    wrap = true,
                )
                MonarchDivider(variant = MonarchDividerVariant.Subtle)
            }
            BasicTextField(
                value = state.value,
                onValueChange = { state.onValueChange(it, maxLength) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = tokens.MinTextHeight)
                    .semantics {
                        contentDescription = label
                        if (overText != null) error(overText)
                    },
                enabled = enabled,
                textStyle = typography.body.copy(color = box.content),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                visualTransformation = transformation,
                interactionSource = interactionSource,
                cursorBrush = SolidColor(tokens.cursor(colors, error)),
                decorationBox = { inner ->
                    Box(Modifier.padding(tokens.TextPadding)) {
                        if (state.text.isEmpty() && prompt != null) {
                            Text(text = prompt, style = typography.body, color = tokens.prompt(colors))
                        }
                        inner()
                    }
                },
            )
        }
        if (showCharacterCount ?: (maxLength != null)) RichTextCount(state.text.length, maxLength, labels, overText)
        RichTextMentions(state, enabled, mentionItems, mentionStatus, labels, onMentionSearch, onMention)
    }
}

private const val CUSTOM = "custom:"

@Composable
private fun richTextNumber(value: Int): String =
    NumberFormat.getIntegerInstance(LocalConfiguration.current.locales[0]).format(value)

@Composable
private fun richTextOver(over: Int, labels: MonarchRichTextEditorLabels): String =
    labels.over?.invoke(over) ?: pluralStringResource(R.plurals.monarch_richtexteditor_over, over, richTextNumber(over))

@Composable
private fun ColumnScope.RichTextCount(
    characters: Int,
    maxLength: Int?,
    labels: MonarchRichTextEditorLabels,
    overText: String?,
) {
    val count = labels.count?.invoke(characters)
        ?: pluralStringResource(R.plurals.monarch_richtexteditor_count, characters, richTextNumber(characters))
    val visible: String
    val spoken: String
    if (maxLength == null) {
        visible = count
        spoken = count
    } else {
        visible = labels.countOfLimit?.invoke(characters, maxLength)
            ?: stringResource(
                R.string.monarch_richtexteditor_count_of_limit,
                richTextNumber(characters),
                richTextNumber(maxLength),
            )
        val left = (maxLength - characters).coerceAtLeast(0)
        spoken = overText ?: labels.remaining?.invoke(left)
            ?: pluralStringResource(R.plurals.monarch_richtexteditor_remaining, left, richTextNumber(left))
    }
    Text(
        text = visible,
        modifier = Modifier
            .align(Alignment.End)
            .semantics { contentDescription = spoken },
        style = MonarchTheme.typography.caption,
        color = MonarchRichTextEditorTokens.count(MonarchTheme.colors, overText != null),
    )
}

@Suppress("LongParameterList")
@Composable
private fun RichTextMentions(
    state: MonarchRichTextEditorState,
    enabled: Boolean,
    items: List<MonarchMentionItem>?,
    status: MonarchMentionPickerStatus,
    labels: MonarchRichTextEditorLabels,
    onMentionSearch: ((MonarchMentionQuery?) -> Unit)?,
    onMention: ((MonarchMentionItem) -> Unit)?,
) {
    val on = items != null && enabled
    val query = if (on) MonarchMentionQuery.find(state.value, MentionTriggers) else null
    LaunchedEffect(query, on) { if (on) onMentionSearch?.invoke(query) }
    if (!on) return
    MonarchMentionPicker(
        query = query,
        items = items.orEmpty(),
        onSelect = { item ->
            if (query != null) {
                state.replaceValue(query.replacing(state.value, "${query.trigger}${item.label} "))
                onMention?.invoke(item)
            }
        },
        status = status,
        onRetry = onMentionSearch?.let { search -> { search(query) } },
        labels = labels.mentions,
    )
}

private val MentionTriggers = setOf('@', '#')
