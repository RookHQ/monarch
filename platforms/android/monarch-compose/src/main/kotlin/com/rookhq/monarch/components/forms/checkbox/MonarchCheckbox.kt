package com.rookhq.monarch.components.forms.checkbox

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.AnnotatedString
import com.rookhq.monarch.accessibility.monarchMinimumTouchTarget
import com.rookhq.monarch.internal.choice.MonarchChoiceRow
import com.rookhq.monarch.internal.choice.toggleSpring
import com.rookhq.monarch.internal.choice.drawChoiceFocusRing
import com.rookhq.monarch.internal.choice.rememberChoiceSource
import com.rookhq.monarch.foundation.MonarchControlMetrics
import com.rookhq.monarch.theme.MonarchTheme

/**
 * A checkbox: the Monarch box (16dp, 4dp corners, 1dp input border, the accent fill with a check mark when on, a
 * dash when partly on) inside a 48dp touch target, with an optional label.
 *
 * It replaces the Material 3 `Checkbox`: same behaviour (`Modifier.toggleable`, `Role.Checkbox`, TalkBack says
 * "checked", "not checked" and, for [MonarchCheckbox] with a [ToggleableState], "partially checked"), another look.
 * The focus ring appears for keyboard, D-pad and switch focus, never for touch.
 *
 * With [label] the whole row is the target and one TalkBack element ("Email me updates, checkbox, checked"); the
 * label wraps at large font sizes. Without it, the box alone is the target; label it from outside (a `Text`
 * next to it in a row that toggles) or pass [label].
 *
 * Pass `onCheckedChange = null` to draw the box only, for a row that handles the tap itself (like Material's
 * `Checkbox`). It then has no touch target and no semantics of its own.
 *
 * Migrating from Material: `Checkbox(checked, onCheckedChange, modifier, enabled, interactionSource = s)` becomes
 * `MonarchCheckbox(checked, onCheckedChange, modifier, enabled, interactionSource = s)`. Remove `colors`.
 *
 * @param checked whether the box is ticked.
 * @param onCheckedChange called with the new value when the user toggles it; `null` draws the box only.
 * @param modifier the modifier of the control.
 * @param enabled whether the user can toggle it. A disabled box takes the subtle fill and a muted mark (no
 * half opacity).
 * @param invalid draws the danger border.
 * @param label the visible label; the row toggles as a whole.
 * @param description secondary text below the label; only with [label].
 * @param interactionSource the source of the press and focus interactions.
 */
@Composable
public fun MonarchCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    invalid: Boolean = false,
    label: String? = null,
    description: String? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    MonarchCheckbox(
        state = ToggleableState(checked),
        onClick = onCheckedChange?.let { change -> { change(!checked) } },
        modifier = modifier,
        enabled = enabled,
        invalid = invalid,
        label = label,
        description = description,
        interactionSource = interactionSource,
    )
}

/**
 * A checkbox with three states, for "select all": off, on, and partly on ([ToggleableState.Indeterminate], drawn
 * as a dash and read by TalkBack as "partially checked"). Everything else is as in the two-state overload.
 *
 * Migrating from Material: `TriStateCheckbox(state, onClick, ...)` becomes `MonarchCheckbox(state, onClick, ...)`.
 *
 * @param state the state of the box.
 * @param onClick called when the user activates it; `null` draws the box only.
 * @param modifier the modifier of the control.
 * @param enabled whether the user can toggle it.
 * @param invalid draws the danger border.
 * @param label the visible label; the row toggles as a whole.
 * @param description secondary text below the label; only with [label].
 * @param interactionSource the source of the press and focus interactions.
 */
@Composable
public fun MonarchCheckbox(
    state: ToggleableState,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    invalid: Boolean = false,
    label: String? = null,
    description: String? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val source = rememberChoiceSource(interactionSource)
    val toggle = if (onClick == null) {
        Modifier
    } else {
        Modifier.triStateToggleable(
            state = state,
            interactionSource = source,
            indication = null,
            enabled = enabled,
            role = Role.Checkbox,
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
            modifier = modifier.then(toggle),
        ) {
            MonarchCheckboxMark(state, enabled, invalid, source, Modifier.clearAndSetSemantics { })
        }
        onClick == null -> MonarchCheckboxMark(state, enabled, invalid, source, modifier)
        else -> Box(
            modifier = modifier.then(toggle).monarchMinimumTouchTarget(),
            contentAlignment = Alignment.Center,
        ) {
            MonarchCheckboxMark(state, enabled, invalid, source, Modifier.clearAndSetSemantics { })
        }
    }
}

/** The 16dp box with its mark and focus ring. It carries no semantics: the row or the target around it does. */
@Composable
internal fun MonarchCheckboxMark(
    state: ToggleableState,
    enabled: Boolean,
    invalid: Boolean,
    source: MutableInteractionSource,
    modifier: Modifier = Modifier,
) {
    val theme = MonarchTheme.colors
    val motion = MonarchTheme.motion
    val focused by source.collectIsFocusedAsState()
    val pressed by source.collectIsPressedAsState()
    val on = state != ToggleableState.Off
    val target = MonarchCheckboxTokens.colors(on, invalid, pressed && enabled, theme, enabled)
    val container by animateColorAsState(target.container, motion.spec(motion.fast), label = "checkbox container")
    val border by animateColorAsState(target.border, motion.spec(motion.fast), label = "checkbox border")
    // The mark is drawn on along its path: a short settle, like a pen stroke, and erased the same way.
    val drawn by animateFloatAsState(if (on) 1f else 0f, motion.toggleSpring(), label = "checkbox mark")
    // Keep the glyph of the last "on" state while the mark is erased.
    val lastOn = remember { arrayOf(state) }
    if (on) lastOn[0] = state
    val points = if (lastOn[0] == ToggleableState.Indeterminate) {
        MonarchCheckboxTokens.DashPoints
    } else {
        MonarchCheckboxTokens.CheckPoints
    }
    val mark = target.mark
    val ring = theme.focusRing
    val ringWidth = MonarchTheme.focusRingWidth
    val path = remember { Path() }
    val partial = remember { Path() }
    val measure = remember { PathMeasure() }
    Canvas(modifier = modifier.size(MonarchControlMetrics.CheckSize)) {
        val corner = MonarchCheckboxTokens.Radius.toPx()
        val stroke = MonarchControlMetrics.BorderWidth.toPx()
        drawRoundRect(container, cornerRadius = CornerRadius(corner))
        drawRoundRect(
            color = border,
            topLeft = Offset(stroke / 2, stroke / 2),
            size = Size(size.width - stroke, size.height - stroke),
            cornerRadius = CornerRadius(corner - stroke / 2),
            style = Stroke(stroke),
        )
        val amount = drawn.coerceIn(0f, 1f)
        if (amount > 0f) {
            val unit = size.width / MonarchCheckboxTokens.GRID
            path.reset()
            points.forEachIndexed { index, point ->
                val x = point.x * unit
                val y = point.y * unit
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            measure.setPath(path, false)
            partial.reset()
            measure.getSegment(0f, measure.length * amount, partial, true)
            drawPath(
                path = partial,
                color = mark,
                style = Stroke(
                    width = MonarchCheckboxTokens.MarkStroke.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }
        if (focused && enabled) drawChoiceFocusRing(ring, ringWidth.toPx(), corner)
    }
}

/**
 * A checkbox row whose label holds links inside the sentence (a [androidx.compose.ui.text.LinkAnnotation] per
 * link). The row toggles as a whole; the links open their own targets and TalkBack lists them.
 */
@Composable
internal fun MonarchCheckboxWithLinks(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: AnnotatedString,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    description: String? = null,
) {
    val source = rememberChoiceSource(null)
    val state = ToggleableState(checked)
    MonarchChoiceRow(
        label = label.text,
        labelContent = label,
        description = description,
        enabled = enabled,
        controlAtEnd = false,
        controlHeight = MonarchControlMetrics.CheckSize,
        modifier = modifier.triStateToggleable(
            state = state,
            interactionSource = source,
            indication = null,
            enabled = enabled,
            role = Role.Checkbox,
            onClick = { onCheckedChange(!checked) },
        ),
    ) {
        MonarchCheckboxMark(state, enabled, false, source, Modifier.clearAndSetSemantics { })
    }
}
