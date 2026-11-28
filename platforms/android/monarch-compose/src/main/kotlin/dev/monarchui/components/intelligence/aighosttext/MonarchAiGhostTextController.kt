package dev.monarchui.components.intelligence.aighosttext

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.LayoutDirection

/** The texts `MonarchAiGhostText` announces, resolved from the labels and the built-in strings. */
internal class GhostTextAnnouncements(
    val announce: (String) -> String,
    val accepted: String,
    val dismissed: String,
)

/**
 * Connects a [TextFieldState] with a [MonarchAiCompletion]: it reports what
 * the field holds, inserts accepted text, handles the hardware keys and keeps
 * the text of the live region.
 */
internal class GhostTextController(private val state: TextFieldState, private val completion: MonarchAiCompletion) {
    /** The text of the polite live region under the field. */
    var announcement: String by mutableStateOf("")
        private set

    var announcements: GhostTextAnnouncements = GhostTextAnnouncements({ it }, "", "")
    var onAccept: ((MonarchAiCompletionAcceptance, MonarchAiGhostTextAcceptKind) -> Unit)? = null
    var onDismiss: (() -> Unit)? = null
    private var announced = ""

    /** Whether a suggestion is showing. */
    val showing: Boolean
        get() = completion.status == MonarchAiCompletionStatus.Ready && completion.suggestion.isNotEmpty()

    /** Tells the completion what the field holds. [active] is false while the field is disabled or unfocused. */
    fun onField(text: String, selection: TextRange, composing: Boolean, active: Boolean) {
        completion.setComposing(composing)
        if (active && !composing && MonarchAiGhostTextTokens.caretAtEnd(text, selection)) {
            if (text != completion.text) announcement = ""
            completion.update(text, selection.start)
        } else {
            completion.dismiss()
        }
    }

    /** Announces a new suggestion; the rest of one being typed through or taken by word is not news. */
    fun onSuggestion() {
        val suggestion = completion.suggestion
        if (!showing) {
            announced = ""
        } else if (!announced.endsWith(suggestion)) {
            announced = suggestion
            announcement = announcements.announce(suggestion.trim())
        }
    }

    /** Inserts the suggestion, or its next word, into the field. */
    fun take(kind: MonarchAiGhostTextAcceptKind) {
        val acceptance = when (kind) {
            MonarchAiGhostTextAcceptKind.All -> completion.accept()
            MonarchAiGhostTextAcceptKind.Word -> completion.acceptWord()
        } ?: return
        val at = acceptance.cursor - acceptance.inserted.length
        state.edit {
            replace(at, at, acceptance.inserted)
            selection = TextRange(acceptance.cursor)
        }
        if (kind == MonarchAiGhostTextAcceptKind.All) announcement = announcements.accepted
        onAccept?.invoke(acceptance, kind)
    }

    /** Drops the suggestion at the user's request. */
    fun dismiss() {
        completion.dismiss()
        announcement = announcements.dismissed
        onDismiss?.invoke()
    }

    /** The hardware keys of a showing suggestion. Returns whether the key was used. */
    fun onKey(event: KeyEvent, direction: LayoutDirection): Boolean {
        val action = if (showing && event.type == KeyEventType.KeyDown) keyAction(event, direction) else null
        when (action) {
            null -> Unit
            KeyAction.Dismiss -> dismiss()
            KeyAction.All -> take(MonarchAiGhostTextAcceptKind.All)
            KeyAction.Word -> take(MonarchAiGhostTextAcceptKind.Word)
        }
        return action != null
    }

    private fun keyAction(event: KeyEvent, direction: LayoutDirection): KeyAction? {
        val forward = if (direction == LayoutDirection.Rtl) Key.DirectionLeft else Key.DirectionRight
        val atSuggestion = state.selection.collapsed && state.selection.start == completion.cursor
        val plain = event.modifiers() == Modifiers.None
        return when {
            event.key == Key.Tab && plain -> KeyAction.All
            event.key == Key.Escape && plain -> KeyAction.Dismiss
            event.key != forward || !atSuggestion -> null
            plain -> KeyAction.All
            event.modifiers() == Modifiers.CtrlOrAlt -> KeyAction.Word
            else -> null
        }
    }

    private fun KeyEvent.modifiers(): Modifiers = when {
        isMetaPressed || isShiftPressed -> Modifiers.Other
        isCtrlPressed || isAltPressed -> Modifiers.CtrlOrAlt
        else -> Modifiers.None
    }

    private enum class Modifiers { None, CtrlOrAlt, Other }

    /** Whether [position], in the coordinates of the text, is on the suggestion or within [slop] of it. */
    fun hitsSuggestion(layout: TextLayoutResult, position: Offset, slop: Float): Boolean {
        val start = completion.cursor
        val end = start + completion.suggestion.length
        if (!showing || end > layout.layoutInput.text.length) return false
        val firstLine = layout.getLineForOffset(start)
        val lastLine = layout.getLineForOffset(end)
        return (firstLine..lastLine).any { line ->
            val left = if (line == firstLine) layout.getHorizontalPosition(start, true) else layout.getLineLeft(line)
            val right = if (line == lastLine) layout.getHorizontalPosition(end, true) else layout.getLineRight(line)
            val inX = position.x in (minOf(left, right) - slop)..(maxOf(left, right) + slop)
            inX && position.y in (layout.getLineTop(line) - slop)..(layout.getLineBottom(line) + slop)
        }
    }

    private enum class KeyAction { All, Word, Dismiss }
}
