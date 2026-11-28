package dev.monarchui.components.forms.radio

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import dev.monarchui.accessibility.monarchMinimumTouchTarget
import dev.monarchui.internal.choice.MonarchChoiceRow
import dev.monarchui.internal.choice.toggleSpring
import dev.monarchui.internal.choice.drawChoiceFocusRing
import dev.monarchui.internal.choice.rememberChoiceSource
import dev.monarchui.foundation.MonarchControlMetrics
import dev.monarchui.theme.MonarchColors
import dev.monarchui.theme.MonarchTheme

/**
 * A radio button: the Monarch ring (16dp, 1dp input border, a 10dp accent dot when selected) inside a 48dp
 * touch target, with an optional label.
 *
 * It replaces the Material 3 `RadioButton`: same behaviour (`Modifier.selectable`, `Role.RadioButton`, TalkBack
 * says "selected" or "not selected"), another look. Put the radios of one question in a [MonarchRadioGroup]
 * so that TalkBack reads each one with its position, "1 of 3". The focus ring appears for keyboard, D-pad and
 * switch focus, never for touch.
 *
 * With [label] the whole row is the target and one TalkBack element; the label wraps at large font sizes.
 * Pass `onClick = null` to draw the ring only, for a row or card that handles the tap itself.
 *
 * Migrating from Material: `RadioButton(selected, onClick, modifier, enabled, interactionSource = s)` becomes
 * `MonarchRadio(selected, onClick, modifier, enabled, interactionSource = s)`. Remove `colors`.
 *
 * @param selected whether this option is the chosen one.
 * @param onClick called when the user chooses it; `null` draws the ring only.
 * @param modifier the modifier of the control.
 * @param enabled whether the user can choose it. A disabled radio is muted (subtle fill, grey dot).
 * @param invalid draws the danger border.
 * @param label the visible label; the row selects as a whole.
 * @param description secondary text below the label; only with [label].
 * @param interactionSource the source of the press and focus interactions.
 */
@Composable
public fun MonarchRadio(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    invalid: Boolean = false,
    label: String? = null,
    description: String? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val source = rememberChoiceSource(interactionSource)
    val select = if (onClick == null) {
        Modifier
    } else {
        Modifier.selectable(
            selected = selected,
            interactionSource = source,
            indication = null,
            enabled = enabled,
            role = Role.RadioButton,
            onClick = onClick,
        )
    }
    when {
        label != null -> MonarchChoiceRow(
            label = label,
            description = description,
            enabled = enabled,
            controlAtEnd = false,
            controlHeight = MonarchControlMetrics.CheckSize,
            modifier = modifier.then(select),
        ) {
            MonarchRadioMark(selected, enabled, invalid, source, Modifier.clearAndSetSemantics { })
        }
        onClick == null -> MonarchRadioMark(selected, enabled, invalid, source, modifier)
        else -> Box(
            modifier = modifier.then(select).monarchMinimumTouchTarget(),
            contentAlignment = Alignment.Center,
        ) {
            MonarchRadioMark(selected, enabled, invalid, source, Modifier.clearAndSetSemantics { })
        }
    }
}

/** The 16dp ring with its dot and focus ring. It carries no semantics: the row or the target around it does. */
@Composable
internal fun MonarchRadioMark(
    selected: Boolean,
    enabled: Boolean,
    invalid: Boolean,
    source: MutableInteractionSource,
    modifier: Modifier = Modifier,
) {
    val theme = MonarchTheme.colors
    val motion = MonarchTheme.motion
    val focused by source.collectIsFocusedAsState()
    val pressed by source.collectIsPressedAsState()
    val ring = radioRing(selected, invalid, pressed && enabled, theme, enabled)
    val container by animateColorAsState(
        radioContainer(pressed && enabled, enabled, theme),
        motion.spec(motion.fast),
        label = "radio container",
    )
    val border by animateColorAsState(ring, motion.spec(motion.fast), label = "radio border")
    // The dot grows in with a small settle and shrinks away the same way.
    val dot by animateFloatAsState(if (selected) 1f else 0f, motion.toggleSpring(), label = "radio dot")
    val dotColor = if (enabled) theme.accentStrong else theme.textDisabled
    val focusColor = theme.focusRing
    val focusWidth = MonarchTheme.focusRingWidth
    Canvas(modifier = modifier.size(MonarchControlMetrics.CheckSize)) {
        val stroke = MonarchControlMetrics.BorderWidth.toPx()
        drawCircle(container)
        drawCircle(
            color = border,
            radius = (size.width - stroke) / 2,
            center = Offset(size.width / 2, size.height / 2),
            style = Stroke(stroke),
        )
        val scale = dot.coerceAtLeast(0f)
        if (scale > 0f) drawCircle(dotColor, radius = MonarchControlMetrics.RadioDotSize.toPx() / 2 * scale)
        if (focused && enabled) drawChoiceFocusRing(focusColor, focusWidth.toPx(), size.width / 2)
    }
}

/** The fill inside the ring: the surface, the muted fill while pressed, the subtle fill when disabled. */
internal fun radioContainer(pressed: Boolean, enabled: Boolean, colors: MonarchColors) = when {
    !enabled -> colors.bgSubtle
    pressed -> colors.bgMuted
    else -> colors.bgSurface
}

/**
 * The ring colour: danger when invalid, the accent when selected, the subtle text colour when pressed, the default
 * border when disabled (a disabled radio is muted, never half opacity).
 */
internal fun radioRing(
    selected: Boolean,
    invalid: Boolean,
    pressed: Boolean,
    colors: MonarchColors,
    enabled: Boolean = true,
) = when {
    !enabled -> colors.borderDefault
    invalid -> colors.dangerStrong
    selected -> colors.accentStrong
    pressed -> colors.textSubtle
    else -> colors.borderInput
}
