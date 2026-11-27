package com.rookhq.monarch.components.intelligence.aighosttext

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.insert
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.editableText
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Dp
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.forms.textfield.MonarchTextFieldTokens
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme

/**
 * A text field with inline completion: the model's suggested continuation is
 * drawn in the muted text colour after the typed text, and accepted by a tap
 * on it, by the "Accept suggestion" button under the field, by the TalkBack
 * action, or by Tab on a hardware keyboard.
 *
 * A Material 3 `OutlinedTextField` on [TextFieldState] in the Monarch colours.
 * The suggestion is added by an `OutputTransformation`, so it is drawn but is
 * never part of [state]. A suggestion is only shown while the field has focus
 * and the caret is at the end of the text, and never while the keyboard is
 * composing a word, so it does not compete with the keyboard's own
 * suggestions. A new suggestion, its acceptance and its dismissal are
 * announced through a polite live region. With a hardware keyboard Tab or the
 * forward arrow accepts, Ctrl or Alt with the forward arrow accepts one word,
 * and Escape dismisses.
 *
 * @param state the text of the field.
 * @param completion the completion state, from [rememberMonarchAiCompletion].
 * @param label the visible label of the field, also its TalkBack label.
 * @param modifier the modifier of the component.
 * @param placeholder an example shown while the field is empty and focused.
 * @param helperText guidance shown below the field.
 * @param enabled whether the user can edit the field. A disabled field shows no suggestion.
 * @param showAcceptButton shows the "Accept suggestion" button under the field while a suggestion shows. Its
 * room is always kept, so nothing moves when it appears.
 * @param lineLimits the lines of the field. The default grows with the text.
 * @param labels replacements for the announcements and the action names.
 * @param onAccept called after a suggestion, or one word of it, went into [state].
 * @param onDismiss called when the user dismissed the suggestion.
 * @param interactionSource the source of the focus and press interactions of the field.
 */
@Composable
public fun MonarchAiGhostText(
    state: TextFieldState,
    completion: MonarchAiCompletion,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    helperText: String? = null,
    enabled: Boolean = true,
    showAcceptButton: Boolean = true,
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.MultiLine(),
    labels: MonarchAiGhostTextLabels = MonarchAiGhostTextLabels(),
    onAccept: ((MonarchAiCompletionAcceptance, MonarchAiGhostTextAcceptKind) -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val active by rememberUpdatedState(enabled && focused)
    val controller = remember(state, completion) { GhostTextController(state, completion) }
    val texts = announcements(labels)
    SideEffect {
        controller.announcements = texts
        controller.onAccept = onAccept
        controller.onDismiss = onDismiss
    }
    val acceptName = monarchString(labels.accept, R.string.monarch_aighosttext_accept)
    val dismissName = monarchString(labels.dismiss, R.string.monarch_aighosttext_dismiss)

    LaunchedEffect(controller) {
        snapshotFlow { Triple(state.text.toString(), state.selection, state.composition != null) to active }
            .collect { (field, isActive) -> controller.onField(field.first, field.second, field.third, isActive) }
    }
    LaunchedEffect(controller, controller.showing, completion.suggestion) { controller.onSuggestion() }

    val output = remember(completion) { SuggestionTransformation(completion) }
    val ghost = MonarchAiGhostTextTokens.suggestionColor(MonarchTheme.colors)
    SideEffect { output.color = ghost }
    val padding = OutlinedTextFieldDefaults.contentPadding()
    val layout = remember { arrayOfNulls<() -> TextLayoutResult?>(1) }
    Column(modifier = modifier) {
        OutlinedTextField(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .ghostTextInput(controller, padding) { layout[0]?.invoke() }
                .semantics {
                    // The suggestion is drawn by the output transformation; TalkBack reads the typed text only.
                    editableText = AnnotatedString(state.text.toString())
                    if (controller.showing) {
                        customActions = listOf(
                            CustomAccessibilityAction(acceptName) {
                                controller.take(MonarchAiGhostTextAcceptKind.All)
                                true
                            },
                            CustomAccessibilityAction(dismissName) {
                                controller.dismiss()
                                true
                            },
                        )
                    }
                },
            enabled = enabled,
            textStyle = MonarchTheme.typography.body,
            label = { Text(label) },
            placeholder = placeholder?.let { { Text(it) } },
            supportingText = helperText?.let { { Text(it) } },
            outputTransformation = output,
            lineLimits = lineLimits,
            onTextLayout = { result -> layout[0] = result },
            shape = MonarchTheme.shapes.md,
            colors = MonarchTextFieldTokens.colors(),
            contentPadding = padding,
            interactionSource = source,
        )
        AcceptRow(controller, showAcceptButton, acceptName)
    }
}

/**
 * A text field with inline completion that asks [complete] for suggestions.
 *
 * The same component as the overload that takes a [MonarchAiCompletion], with the completion remembered
 * here. Use the other overload to read or drive the completion yourself.
 *
 * @param state the text of the field.
 * @param label the visible label of the field, also its TalkBack label.
 * @param modifier the modifier of the component.
 * @param placeholder an example shown while the field is empty and focused.
 * @param helperText guidance shown below the field.
 * @param enabled whether the user can edit the field.
 * @param completionEnabled whether suggestions are made.
 * @param debounceMillis how long typing must pause before a request is sent.
 * @param minimumCharacters the fewest characters before the cursor for a request.
 * @param showAcceptButton shows the "Accept suggestion" button under the field while a suggestion shows.
 * @param lineLimits the lines of the field.
 * @param labels replacements for the announcements and the action names.
 * @param onAccept called after a suggestion, or one word of it, went into [state].
 * @param onDismiss called when the user dismissed the suggestion.
 * @param interactionSource the source of the focus and press interactions of the field.
 * @param complete asks the model for a continuation of the text at the cursor, a UTF-16 offset. Return the text
 * to insert, or `null` for none. It is cancelled when its answer is stale.
 */
@Composable
public fun MonarchAiGhostText(
    state: TextFieldState,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    helperText: String? = null,
    enabled: Boolean = true,
    completionEnabled: Boolean = true,
    debounceMillis: Long = MonarchAiCompletion.DEFAULT_DEBOUNCE_MILLIS,
    minimumCharacters: Int = MonarchAiCompletion.DEFAULT_MINIMUM_CHARACTERS,
    showAcceptButton: Boolean = true,
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.MultiLine(),
    labels: MonarchAiGhostTextLabels = MonarchAiGhostTextLabels(),
    onAccept: ((MonarchAiCompletionAcceptance, MonarchAiGhostTextAcceptKind) -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    complete: suspend (text: String, cursor: Int) -> String?,
) {
    MonarchAiGhostText(
        state = state,
        completion = rememberMonarchAiCompletion(debounceMillis, minimumCharacters, completionEnabled, complete),
        label = label,
        modifier = modifier,
        placeholder = placeholder,
        helperText = helperText,
        enabled = enabled,
        showAcceptButton = showAcceptButton,
        lineLimits = lineLimits,
        labels = labels,
        onAccept = onAccept,
        onDismiss = onDismiss,
        interactionSource = interactionSource,
    )
}

/** Always in the layout: it keeps the room of the accept button and announces through a live region. */
@Composable
private fun AcceptRow(controller: GhostTextController, showAcceptButton: Boolean, acceptName: String) {
    val announcement = controller.announcement
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (showAcceptButton) MonarchAiGhostTextTokens.AcceptRowHeight else Dp.Unspecified)
            .semantics {
                liveRegion = LiveRegionMode.Polite
                if (announcement.isNotEmpty()) contentDescription = announcement
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        if (showAcceptButton && controller.showing) {
            MonarchButton(
                text = acceptName,
                onClick = { controller.take(MonarchAiGhostTextAcceptKind.All) },
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
            )
        }
    }
}

@Composable
private fun announcements(labels: MonarchAiGhostTextLabels): GhostTextAnnouncements {
    val template = stringResource(R.string.monarch_aighosttext_announce)
    return GhostTextAnnouncements(
        announce = labels.announce ?: { suggestion -> template.format(suggestion) },
        accepted = monarchString(labels.accepted, R.string.monarch_aighosttext_accepted),
        dismissed = monarchString(labels.dismissed, R.string.monarch_aighosttext_dismissed),
    )
}

/** The hardware keys, and a tap on the suggestion that accepts it instead of moving the caret. */
@Composable
private fun Modifier.ghostTextInput(
    controller: GhostTextController,
    padding: PaddingValues,
    layout: () -> TextLayoutResult?,
): Modifier {
    val direction = LocalLayoutDirection.current
    val slop = MonarchAiGhostTextTokens.TapSlop
    return onPreviewKeyEvent { controller.onKey(it, direction) }
        .pointerInput(controller, direction) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                val origin = Offset(
                    padding.calculateLeftPadding(direction).toPx(),
                    padding.calculateTopPadding().toPx(),
                )
                val result = layout()
                if (result != null && controller.hitsSuggestion(result, down.position - origin, slop.toPx())) {
                    down.consume()
                    waitForUpOrCancellation(PointerEventPass.Initial)?.let { up ->
                        up.consume()
                        controller.take(MonarchAiGhostTextAcceptKind.All)
                    }
                }
            }
        }
}

/** Draws the suggestion after the caret in [SuggestionTransformation.color]. Reads snapshot state, so it follows. */
private class SuggestionTransformation(private val completion: MonarchAiCompletion) : OutputTransformation {
    var color: Color by mutableStateOf(Color.Unspecified)

    override fun TextFieldBuffer.transformOutput() {
        val suggestion = completion.suggestion
        val at = completion.cursor
        val ready = completion.status == MonarchAiCompletionStatus.Ready && suggestion.isNotEmpty()
        if (ready && at <= length && asCharSequence().toString() == completion.text) {
            insert(at, suggestion)
            addStyle(SpanStyle(color = color), at, at + suggestion.length)
        }
    }
}
