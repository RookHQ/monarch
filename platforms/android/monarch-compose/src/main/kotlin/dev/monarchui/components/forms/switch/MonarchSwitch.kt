package dev.monarchui.components.forms.switch

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.lerp
import dev.monarchui.accessibility.monarchMinimumTouchTarget
import dev.monarchui.internal.choice.MonarchChoiceRow
import dev.monarchui.internal.choice.drawThumbHairline
import dev.monarchui.internal.choice.rememberToggleHaptics
import dev.monarchui.internal.choice.toggleSpring
import dev.monarchui.internal.choice.drawChoiceFocusRing
import dev.monarchui.internal.choice.rememberChoiceSource
import dev.monarchui.foundation.MonarchControlMetrics
import dev.monarchui.foundation.layerColors
import dev.monarchui.theme.MonarchTheme

/**
 * A switch for a setting that takes effect at once: the Material 52 by 32dp touch-friendly track, in Monarch
 * colours (on: the accent track with a white thumb in every theme; off: the muted track with the input border and a
 * subtle thumb), inside a 48dp touch target, with an optional label at the start.
 *
 * It replaces the Material 3 `Switch`: same behaviour (`Modifier.toggleable`, `Role.Switch`, TalkBack says "on"
 * or "off"), same size, Monarch look. The focus ring appears for keyboard, D-pad and switch focus, never for
 * touch. The thumb moves the other way in right-to-left layouts. It moves on a spring with a small settle, and a
 * toggle gives the platform's "on" or "off" haptic (when the system's touch feedback is on).
 *
 * With [label] the whole row is the target and one TalkBack element ("Alerts, switch, on"); the label wraps at
 * large font sizes. Pass `onCheckedChange = null` to draw the switch only, for a row that handles the tap itself.
 *
 * Migrating from Material: `Switch(checked, onCheckedChange, modifier, enabled = e, interactionSource = s)`
 * becomes `MonarchSwitch(checked, onCheckedChange, modifier, enabled = e, interactionSource = s)`. Remove
 * `colors` and `thumbContent`.
 *
 * @param checked whether the switch is on.
 * @param onCheckedChange called with the new value when the user toggles it; `null` draws the switch only.
 * @param modifier the modifier of the control.
 * @param enabled whether the user can toggle it. A disabled switch is muted (grey track and thumb).
 * @param label the visible label, at the start; the row toggles as a whole.
 * @param description secondary text below the label; only with [label].
 * @param interactionSource the source of the press and focus interactions.
 */
@Composable
public fun MonarchSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String? = null,
    description: String? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val source = rememberChoiceSource(interactionSource)
    val onToggle = rememberToggleHaptics(onCheckedChange)
    val toggle = if (onToggle == null) {
        Modifier
    } else {
        Modifier.toggleable(
            value = checked,
            interactionSource = source,
            indication = null,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = onToggle,
        )
    }
    when {
        label != null -> MonarchChoiceRow(
            label = label,
            description = description,
            enabled = enabled,
            controlAtEnd = true,
            controlHeight = MonarchControlMetrics.SwitchTrackHeight,
            modifier = modifier.then(toggle),
        ) {
            MonarchSwitchTrack(checked, enabled, source, Modifier.clearAndSetSemantics { })
        }
        onCheckedChange == null -> MonarchSwitchTrack(checked, enabled, source, modifier)
        else -> Box(
            modifier = modifier.then(toggle).monarchMinimumTouchTarget(),
            contentAlignment = Alignment.Center,
        ) {
            MonarchSwitchTrack(checked, enabled, source, Modifier.clearAndSetSemantics { })
        }
    }
}

/** The track, the thumb and the focus ring. It carries no semantics: the row or the target around it does. */
@Composable
internal fun MonarchSwitchTrack(
    checked: Boolean,
    enabled: Boolean,
    source: MutableInteractionSource,
    modifier: Modifier = Modifier,
) {
    val theme = MonarchTheme.colors
    val motion = MonarchTheme.motion
    val focused by source.collectIsFocusedAsState()
    val pressed by source.collectIsPressedAsState()
    val colors = MonarchSwitchTokens.colors(checked, theme, enabled)
    val track by animateColorAsState(colors.track, motion.spec(motion.base), label = "switch track")
    val border by animateColorAsState(colors.border, motion.spec(motion.base), label = "switch border")
    val thumb by animateColorAsState(colors.thumb, motion.spec(motion.base), label = "switch thumb")
    // The thumb travels on a spring with a small settle (under a dp, inside the track); colours cross-fade.
    val progress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = motion.toggleSpring(),
        label = "switch position",
    )
    val grow by animateFloatAsState(if (pressed && enabled) 1f else 0f, motion.toggleSpring(), label = "thumb press")
    val dark = MonarchTheme.isDark
    val shade = MonarchTheme.elevation.sm.layerColors
    val ring = theme.focusRing
    val ringWidth = MonarchTheme.focusRingWidth
    Canvas(modifier = modifier.size(MonarchControlMetrics.SwitchTrackWidth, MonarchControlMetrics.SwitchTrackHeight)) {
        val stroke = MonarchControlMetrics.BorderWidth.toPx()
        val radius = size.height / 2
        drawRoundRect(track, cornerRadius = CornerRadius(radius))
        drawRoundRect(
            color = border,
            topLeft = Offset(stroke / 2, stroke / 2),
            size = Size(size.width - stroke, size.height - stroke),
            cornerRadius = CornerRadius(radius - stroke / 2),
            style = Stroke(stroke),
        )
        val diameter = MonarchSwitchTokens.thumbDiameter(progress, grow).toPx()
        // The thumb is centred on the track's end caps: its centre moves from one cap's centre to the other's.
        val centre = lerp(radius, size.width - radius, progress)
        val x = if (layoutDirection == LayoutDirection.Rtl) size.width - centre else centre
        val thumbCentre = Offset(x, size.height / 2)
        // Only the light "on" thumb takes the hairline; the grey "off" thumb sits flat in its track.
        drawThumbHairline(thumbCentre, diameter / 2, shade, dark, strength = if (enabled) progress else 0f)
        drawCircle(thumb, radius = diameter / 2, center = thumbCentre)
        if (focused && enabled) drawChoiceFocusRing(ring, ringWidth.toPx(), radius)
    }
}
