package dev.monarchui.components.forms.taginput

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.datadisplay.tag.MonarchTag
import dev.monarchui.components.datadisplay.tag.MonarchTagSize
import dev.monarchui.components.forms.formfield.MonarchFieldLabel
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchSurface
import dev.monarchui.theme.MonarchTheme

/** A change that is announced. */
private sealed interface TagEvent {
    data class Added(val tags: List<String>) : TagEvent
    data class Removed(val tag: String) : TagEvent
    data class Rejected(val rejection: MonarchTagRejection) : TagEvent
}

/**
 * A field that edits a list of short values as removable tags: skills,
 * labels, email recipients.
 *
 * Built by Monarch from a `FlowRow` of `MonarchTag`s and a `BasicTextField`
 * in one box: Material 3 has no tag field. Typing a separator (a comma by
 * default) or pressing Done adds the typed text; a paste that contains
 * separators, new lines or tabs adds every part. A tag that is already there
 * (without case), one over [maxCount] or one [validate] refuses stays in the
 * field with a message in the danger colour, which is announced and clears
 * when the user types. Matching [suggestions] are offered as buttons under
 * the box. The tags are hoisted; the typed text is kept by the field.
 *
 * @param tags the tags, in order.
 * @param onTagsChange called with the new list when a tag is added or removed.
 * @param label the visible label, and the TalkBack name of the text field.
 * @param required shows the required mark after the label.
 * @param removeIcon the close mark of each tag, for example a `MonarchIcon` of size `Sm` with a `null` content
 * description. Monarch bundles no icons, so the app passes one; it is decorative.
 * @param modifier the modifier of the field.
 * @param enabled whether the user can add and remove tags. A disabled field shows the tags without remove.
 * @param readOnly shows the tags without remove and without the text field.
 * @param placeholder text in the empty text field, for example "Add a skill".
 * @param helperText help under the box, for example how values are separated.
 * @param errorText an error from the app, shown like a refusal while no tag is refused. A blank string is no error.
 * @param suggestions values offered while the typed text matches them (without case); added values are left
 * out and at most eight show.
 * @param separators the characters that add the typed text, besides Done. New lines and tabs also split a paste.
 * @param maxCount the most tags; `null` for no limit.
 * @param allowDuplicates whether the same tag may appear twice.
 * @param normalize cleans typed text before it is checked and added. Trims white space by default.
 * @param validate returns a message to refuse a tag, or `null` to accept it. Runs after the duplicate and
 * maximum checks, with the tag and the current tags.
 * @param suggestionIcon an icon before each suggestion, for example a plus `MonarchIcon` of size `Sm`.
 * @param errorIcon an icon before the error message, for example a warning `MonarchIcon` of size `Sm` with a
 * `null` content description. The message is text either way.
 * @param labels replacements for the built-in messages, announcements and TalkBack names.
 * @param interactionSource the source of the focus interactions of the text field.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
public fun MonarchTagInput(
    tags: List<String>,
    onTagsChange: (List<String>) -> Unit,
    label: String,
    removeIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    placeholder: String? = null,
    helperText: String? = null,
    errorText: String? = null,
    suggestions: List<String> = emptyList(),
    separators: Set<Char> = setOf(','),
    maxCount: Int? = null,
    allowDuplicates: Boolean = false,
    normalize: ((String) -> String)? = null,
    validate: ((String, List<String>) -> String?)? = null,
    suggestionIcon: (@Composable () -> Unit)? = null,
    errorIcon: (@Composable () -> Unit)? = null,
    labels: MonarchTagInputLabels = MonarchTagInputLabels(),
    interactionSource: MutableInteractionSource? = null,
    required: Boolean = false,
) {
    val tokens = MonarchTagInputTokens
    val colors = MonarchTheme.colors
    val rules = MonarchTagRules(normalize, maxCount, allowDuplicates, validate)
    var text by rememberSaveable { mutableStateOf("") }
    var rejection by remember { mutableStateOf<MonarchTagRejection?>(null) }
    var event by remember { mutableStateOf<TagEvent?>(null) }
    val focusRequester = remember { FocusRequester() }
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()

    fun add(parts: List<String>, remainder: String) {
        val result = MonarchTagInputLogic.commit(parts, tags, rules)
        if (result.added.isNotEmpty()) onTagsChange(result.tags)
        rejection = result.rejection
        text = fieldText(result, remainder)
        event = eventOf(result) ?: event
    }

    val message = rejection?.let { rejectionMessage(it, labels) } ?: errorText?.takeIf { it.isNotBlank() }
    val announcement = event?.let { announcementOf(it, labels) }
    val interactive = enabled && !readOnly
    val boxColors = tokens.boxColors(enabled, readOnly, focused, message != null, colors)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(tokens.Gap)) {
        MonarchFieldLabel(label, enabled, required, null)
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .monarchSurface(boxColors, MonarchShapeDefaults.Control)
                .padding(tokens.BoxPadding),
            horizontalArrangement = Arrangement.spacedBy(tokens.TagGap),
            verticalArrangement = Arrangement.spacedBy(tokens.TagGap),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Tags(tags, interactive, removeIcon, labels) { index ->
                val tag = tags[index]
                onTagsChange(tags.filterIndexed { i, _ -> i != index })
                event = TagEvent.Removed(tag)
                focusRequester.requestFocus()
            }
            if (!readOnly) {
                TagTextField(
                    text = text,
                    onTextChange = { next ->
                        rejection = null
                        val edit = MonarchTagInputLogic.input(text, next, separators)
                        if (edit.parts.isEmpty()) text = edit.remainder else add(edit.parts, edit.remainder)
                    },
                    onDone = { add(listOf(text), "") },
                    label = label,
                    message = message,
                    placeholder = placeholder,
                    enabled = enabled,
                    contentColor = boxColors.content,
                    focusRequester = focusRequester,
                    interactionSource = source,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        SupportingText(message = message, helperText = helperText, enabled = enabled, errorIcon = errorIcon)
        val offered = if (interactive) MonarchTagInputLogic.suggestions(suggestions, text, tags) else emptyList()
        Suggestions(offered, suggestionIcon, labels) { suggestion ->
            add(listOf(suggestion), "")
            focusRequester.requestFocus()
        }
        // Always present, so each addition, removal and refusal is announced once.
        Box(
            modifier = Modifier.semantics {
                liveRegion = LiveRegionMode.Polite
                if (announcement != null) contentDescription = announcement
            },
        )
    }
}

/** The text left in the field after adding: the refused parts, then what was typed after the last separator. */
private fun fieldText(result: MonarchTagCommit, remainder: String): String =
    (result.rejected + remainder.takeIf { it.isNotBlank() }).filterNotNull()
        .joinToString(MonarchTagInputLogic.REJECTED_JOIN)

/** What adding announces: the refusal, or the added tags, or nothing. */
private fun eventOf(result: MonarchTagCommit): TagEvent? = when {
    result.rejection != null -> TagEvent.Rejected(result.rejection)
    result.added.isNotEmpty() -> TagEvent.Added(result.added)
    else -> null
}

@Composable
private fun Tags(
    tags: List<String>,
    interactive: Boolean,
    removeIcon: @Composable () -> Unit,
    labels: MonarchTagInputLabels,
    onRemove: (Int) -> Unit,
) {
    tags.forEachIndexed { index, tag ->
        if (interactive) {
            MonarchTag(
                text = tag,
                onRemove = { onRemove(index) },
                removeIcon = removeIcon,
                size = MonarchTagSize.Sm,
                removeLabel = labels.remove?.invoke(tag),
            )
        } else {
            MonarchTag(text = tag, size = MonarchTagSize.Sm)
        }
    }
}

@Composable
private fun TagTextField(
    text: String,
    onTextChange: (String) -> Unit,
    onDone: () -> Unit,
    label: String,
    message: String?,
    placeholder: String?,
    enabled: Boolean,
    contentColor: Color,
    focusRequester: FocusRequester,
    interactionSource: MutableInteractionSource,
    modifier: Modifier,
) {
    val tokens = MonarchTagInputTokens
    val colors = MonarchTheme.colors
    BasicTextField(
        value = text,
        onValueChange = onTextChange,
        modifier = modifier
            .widthIn(min = tokens.FieldMinWidth)
            .heightIn(min = tokens.FieldMinHeight)
            .focusRequester(focusRequester)
            .semantics {
                contentDescription = label
                if (message != null) error(message)
            },
        enabled = enabled,
        textStyle = MonarchTheme.typography.label.copy(fontWeight = FontWeight.Normal, color = contentColor),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        singleLine = true,
        interactionSource = interactionSource,
        cursorBrush = SolidColor(tokens.cursorColor(colors)),
        decorationBox = { field ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (text.isEmpty() && placeholder != null) {
                    Text(
                        text = placeholder,
                        style = MonarchTheme.typography.label.copy(fontWeight = FontWeight.Normal),
                        color = tokens.helperColor(enabled, colors),
                    )
                }
                field()
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Suggestions(
    offered: List<String>,
    icon: (@Composable () -> Unit)?,
    labels: MonarchTagInputLabels,
    onAdd: (String) -> Unit,
) {
    if (offered.isEmpty()) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(MonarchTagInputTokens.TagGap)) {
        offered.forEach { suggestion ->
            val name = labels.addSuggestion?.invoke(suggestion)
                ?: stringResource(R.string.monarch_taginput_add_suggestion, suggestion)
            MonarchButton(
                text = suggestion,
                onClick = { onAdd(suggestion) },
                modifier = Modifier.semantics { contentDescription = name },
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
                leadingIcon = icon,
            )
        }
    }
}

@Composable
private fun SupportingText(
    message: String?,
    helperText: String?,
    enabled: Boolean,
    errorIcon: (@Composable () -> Unit)?,
) {
    val tokens = MonarchTagInputTokens
    val colors = MonarchTheme.colors
    if (message != null) {
        Row(
            modifier = Modifier.clearAndSetSemantics { },
            horizontalArrangement = Arrangement.spacedBy(tokens.InlineGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (errorIcon != null) {
                CompositionLocalProvider(LocalContentColor provides tokens.errorColor(colors)) { errorIcon() }
            }
            Text(text = message, style = MonarchTheme.typography.caption, color = tokens.errorColor(colors))
        }
    } else if (helperText != null) {
        Text(text = helperText, style = MonarchTheme.typography.caption, color = tokens.helperColor(enabled, colors))
    }
}

@Composable
private fun rejectionMessage(rejection: MonarchTagRejection, labels: MonarchTagInputLabels): String =
    when (rejection) {
        is MonarchTagRejection.Duplicate -> labels.duplicate?.invoke(rejection.tag)
            ?: stringResource(R.string.monarch_taginput_duplicate, rejection.tag)
        is MonarchTagRejection.Maximum -> labels.maximum?.invoke(rejection.maximum)
            ?: stringResource(R.string.monarch_taginput_maximum, rejection.maximum)
        is MonarchTagRejection.Invalid -> rejection.message
    }

@Composable
private fun announcementOf(event: TagEvent, labels: MonarchTagInputLabels): String = when (event) {
    is TagEvent.Added -> event.tags.map { tag ->
        labels.added?.invoke(tag) ?: stringResource(R.string.monarch_taginput_added, tag)
    }.joinToString(" ")
    is TagEvent.Removed -> labels.removed?.invoke(event.tag)
        ?: stringResource(R.string.monarch_taginput_removed, event.tag)
    is TagEvent.Rejected -> rejectionMessage(event.rejection, labels)
}
