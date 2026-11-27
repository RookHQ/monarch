package com.rookhq.monarch.components.intelligence.aistopbutton

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme

/**
 * The Stop control of a streaming assistant response, and its stopped state.
 *
 * A `MonarchButton` (secondary) with a filled square and the title "Stop".
 * When [stopped] is `true` the button is replaced by the muted text
 * "Stopped." and, with [onContinue], a ghost Continue button. The stopped text
 * is a polite live region that stays in the layout, so the change is announced
 * by TalkBack without moving focus. If the Stop button had keyboard focus when
 * it was replaced, focus moves to Continue.
 *
 * The component does not track the request: it shows Stop until the app sets
 * [stopped]. Wire Escape or other shortcuts to the same [onStop] in the screen
 * that owns the request.
 *
 * @param onStop called when the user activates Stop. Abort the request here.
 * @param modifier the modifier of the component.
 * @param size the height of the Stop and Continue buttons. The touch target is at least 48dp at every size.
 * @param enabled whether Stop can be activated, for example `false` while the abort is in flight.
 * @param stopped whether the response has stopped. Replaces Stop with the stopped text.
 * @param showLabel shows the title beside the square. When `false` the title is still the TalkBack name.
 * @param onContinue adds a Continue button to the stopped state, for example to resume or regenerate.
 * @param stopLabel replaces the built-in title `monarch_aistopbutton_stop`, "Stop".
 * @param stoppedLabel replaces the built-in text `monarch_aistopbutton_stopped`, "Stopped.".
 * @param continueLabel replaces the built-in title `monarch_aistopbutton_continue`, "Continue".
 * @param interactionSource the source of the press, focus and hover interactions of the Stop button.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
public fun MonarchAiStopButton(
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
    size: MonarchButtonSize = MonarchButtonSize.Sm,
    enabled: Boolean = true,
    stopped: Boolean = false,
    showLabel: Boolean = true,
    onContinue: (() -> Unit)? = null,
    stopLabel: String? = null,
    stoppedLabel: String? = null,
    continueLabel: String? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val stopText = monarchString(stopLabel, R.string.monarch_aistopbutton_stop)
    val stoppedText = monarchString(stoppedLabel, R.string.monarch_aistopbutton_stopped)
    val continueText = monarchString(continueLabel, R.string.monarch_aistopbutton_continue)
    // Plain holder, not state: it is only read when the stopped state changes.
    val stopHadFocus = remember { booleanArrayOf(false) }
    val isStopped by rememberUpdatedState(stopped)
    val continueFocus = remember { FocusRequester() }
    val showsContinue = stopped && onContinue != null
    FlowRow(modifier = modifier, itemVerticalAlignment = Alignment.CenterVertically) {
        if (!stopped) {
            MonarchButton(
                onClick = onStop,
                modifier = Modifier
                    .onFocusChanged {
                        // Removing the focused button also reports a loss of focus; keep what it had.
                        if (it.isFocused) stopHadFocus[0] = true else if (!isStopped) stopHadFocus[0] = false
                    }
                    .semantics { if (!showLabel) contentDescription = stopText },
                variant = MonarchButtonVariant.Secondary,
                size = size,
                enabled = enabled,
                interactionSource = interactionSource,
            ) {
                Box(
                    modifier = Modifier
                        .size(MonarchAiStopButtonTokens.glyphSize(size))
                        .background(LocalContentColor.current, MonarchAiStopButtonTokens.GlyphShape),
                )
                if (showLabel) Text(text = stopText)
            }
        }
        // Always in the layout, so the stopped text is announced as a change of a live region.
        Text(
            text = if (stopped) stoppedText else "",
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = MonarchTheme.typography.body,
            color = MonarchAiStopButtonTokens.stoppedTextColor(MonarchTheme.colors),
        )
        if (stopped && onContinue != null) {
            MonarchButton(
                text = continueText,
                onClick = onContinue,
                modifier = Modifier
                    .padding(start = MonarchAiStopButtonTokens.Gap)
                    .focusRequester(continueFocus),
                variant = MonarchButtonVariant.Ghost,
                size = size,
            )
        }
    }
    LaunchedEffect(showsContinue) {
        if (showsContinue && stopHadFocus[0]) {
            stopHadFocus[0] = false
            continueFocus.requestFocus()
        }
    }
}
