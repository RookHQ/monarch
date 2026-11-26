package com.rookhq.monarch.components.intelligence.aivoiceinput

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import com.rookhq.monarch.accessibility.monarchFocusRing
import com.rookhq.monarch.accessibility.monarchMinimumTouchTarget
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonTokens
import com.rookhq.monarch.theme.MonarchTheme

/**
 * The push-to-talk button of `MonarchAiVoiceInput`: it listens while touched
 * and held, or while Enter or Space is held on a keyboard.
 *
 * It looks like `MonarchButton`. A Material 3 button reports a click, not a
 * press and a release, so this is a `Surface` with a press gesture. TalkBack
 * and Switch Access cannot hold, so its click action ([onToggle]) starts and
 * stops instead, and it reads as on while listening.
 */
@Composable
internal fun MonarchAiVoiceInputHoldButton(
    text: String,
    hint: String,
    active: Boolean,
    size: MonarchButtonSize,
    enabled: Boolean,
    icon: (@Composable () -> Unit)?,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val press by rememberUpdatedState(onPress)
    val release by rememberUpdatedState(onRelease)
    val variant = MonarchAiVoiceInputTokens.buttonVariant(active)
    val colors = MonarchButtonTokens.colors(variant, enabled, MonarchTheme.colors)
    val shape = MonarchTheme.shapes.md
    Surface(
        modifier = modifier
            .monarchMinimumTouchTarget()
            .monarchFocusRing(shape)
            .heightIn(min = MonarchButtonTokens.minHeight(size))
            .pointerInput(enabled) {
                if (enabled) {
                    detectTapGestures(
                        onPress = {
                            press()
                            tryAwaitRelease()
                            release()
                        },
                    )
                }
            }
            .onKeyEvent { event ->
                val hold = event.key == Key.Enter || event.key == Key.Spacebar || event.key == Key.NumPadEnter
                when {
                    !enabled || !hold -> false
                    event.type == KeyEventType.KeyDown -> {
                        if (!active) press()
                        true
                    }
                    event.type == KeyEventType.KeyUp -> {
                        release()
                        true
                    }
                    else -> false
                }
            }
            .focusable(enabled)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                toggleableState = ToggleableState(active)
                onClick(label = hint) {
                    if (enabled) onToggle()
                    enabled
                }
                if (!enabled) disabled()
            },
        shape = shape,
        color = colors.container,
        contentColor = colors.content,
        border = MonarchButtonTokens.border(colors),
    ) {
        Row(
            modifier = Modifier.padding(MonarchButtonTokens.contentPadding(size)),
            horizontalArrangement = Arrangement.spacedBy(MonarchButtonTokens.ContentGap, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon?.invoke()
            Text(text = text, style = MonarchButtonTokens.textStyle(size, MonarchTheme.typography))
        }
    }
}
