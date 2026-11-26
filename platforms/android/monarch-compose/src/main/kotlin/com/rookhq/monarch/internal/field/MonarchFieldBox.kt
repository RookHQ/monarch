package com.rookhq.monarch.internal.field

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rookhq.monarch.accessibility.monarchMinimumTouchTarget
import com.rookhq.monarch.foundation.MonarchControlMetrics
import com.rookhq.monarch.foundation.MonarchFieldSize
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.internal.monarchSurface
import com.rookhq.monarch.theme.MonarchTheme
import com.rookhq.monarch.tokens.MonarchTokens

/** The size of an icon inside a field's box: 20dp, outlined, in the muted colour (PREMIUM rule 9). */
internal val MonarchFieldIconSize = MonarchTokens.Space.space5.dp

/**
 * The size Monarch's own field glyphs take where they are drawn: [MonarchFieldIconSize] in a field's leading and
 * trailing slots, the 16dp mark size elsewhere (the error line, menu rows).
 */
internal val LocalMonarchFieldGlyphSize = staticCompositionLocalOf { MonarchControlMetrics.IconSize }

/**
 * The text style of a field's value, placeholder and prefix: the body role at the field size, 14sp, in `sp`
 * so it follows the font scale.
 */
@Composable
internal fun monarchFieldTextStyle(): TextStyle =
    MonarchTheme.typography.body.copy(
        fontSize = MonarchTokens.FontSize.sm.sp,
        lineHeight = (MonarchTokens.FontSize.sm * MonarchTokens.LineHeight.normal).sp,
    )

/**
 * The box of a Monarch field: a 1px border, radius `md`, the hover, focus, invalid and disabled colours of the
 * web, and slots for a leading element, a prefix, the content, a suffix and a trailing element.
 *
 * It draws the box only. The label above and the helper or error text below belong to the form field around
 * it. The caret, selection, keyboard and autofill belong to the text engine inside [content] (a
 * `BasicTextField`), which the box does not replace.
 *
 * Layout: the box is at least [MonarchControlMetrics.fieldMinHeight] tall (40, 48 or 52dp) and grows with
 * the text, so a larger font scale never clips. A small field reserves a 48dp hit area. The row is
 * `[leading] [prefix] [content: weight 1] [suffix] [trailing]`; the slots are vertically centred, and Monarch's
 * own glyphs in them are drawn at [MonarchFieldIconSize].
 *
 * @param modifier applied to the outer element, which also carries the 48dp touch target.
 * @param size the height step.
 * @param state the interaction and validation state; see [rememberMonarchFieldState] to derive it.
 * @param multiline top-aligns the slots and raises the minimum height to the text area height.
 * @param leading an icon or other element before the text, tinted `textMuted`.
 * @param prefix text before the value, for example a currency sign.
 * @param suffix text after the value, for example a unit.
 * @param trailing an icon or button after the text, tinted `textMuted`.
 * @param content the value area. Compose it with a `BasicTextField` and, for an empty value,
 * [MonarchFieldPlaceholder] in a `Box`.
 */
@Composable
internal fun MonarchFieldBox(
    modifier: Modifier = Modifier,
    size: MonarchFieldSize = MonarchFieldSize.Medium,
    state: MonarchFieldState = MonarchFieldState(),
    multiline: Boolean = false,
    shape: Shape = MonarchShapeDefaults.Control,
    leading: (@Composable () -> Unit)? = null,
    prefix: String? = null,
    suffix: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val fieldColors = MonarchTheme.colors.fieldColors(state)
    val tinted = LocalMonarchFieldAiTint.current && state.enabled && !state.invalid && !state.focused
    val colors = if (tinted) {
        fieldColors.copy(container = MonarchTheme.colors.aiSubtle, border = MonarchTheme.colors.aiBorder)
    } else {
        fieldColors
    }
    val minHeight = if (multiline) {
        MonarchControlMetrics.TextAreaMinHeight
    } else {
        MonarchControlMetrics.fieldMinHeight(size)
    }
    val horizontal = MonarchControlMetrics.fieldHorizontalPadding(size)
    val textStyle = monarchFieldTextStyle()
    Box(modifier.monarchMinimumTouchTarget()) {
        Row(
            Modifier
                .fieldRing(colors.ring, shape, MonarchTheme.focusRingWidth)
                .monarchSurface(
                    MonarchSurfaceColors(container = colors.container, content = colors.text, border = colors.border),
                    shape,
                )
                .heightIn(min = minHeight)
                .padding(
                    horizontal = horizontal,
                    vertical = if (multiline) MonarchControlMetrics.MultilineVerticalPadding else 0.dp,
                ),
            verticalAlignment = if (multiline) Alignment.Top else Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MonarchControlMetrics.FieldGap),
        ) {
            leading?.let { Slot(colors.icon, multiline, it) }
            prefix?.let { Affix(it, colors.affix, textStyle) }
            Box(Modifier.weight(1f), contentAlignment = if (multiline) Alignment.TopStart else Alignment.CenterStart) {
                CompositionLocalProvider(
                    LocalContentColor provides colors.text,
                    LocalTextStyle provides textStyle.copy(color = colors.text),
                    content = content,
                )
            }
            suffix?.let { Affix(it, colors.affix, textStyle) }
            trailing?.let { Slot(colors.icon, multiline, it) }
        }
    }
}

/** The placeholder of an empty field, in `textSubtle` (`textDisabled` when disabled), one line. */
@Composable
internal fun MonarchFieldPlaceholder(text: String, enabled: Boolean = true) {
    val colors = MonarchTheme.colors
    Text(
        text = text,
        style = monarchFieldTextStyle(),
        color = if (enabled) colors.textSubtle else colors.textDisabled,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Derives the [MonarchFieldState] from an interaction source (focus, hover) and the flags a field is given. */
@Composable
internal fun rememberMonarchFieldState(
    interactionSource: InteractionSource,
    enabled: Boolean,
    invalid: Boolean = false,
    success: Boolean = false,
): MonarchFieldState {
    val focused by interactionSource.collectIsFocusedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    // A control inside a MonarchFormField with an error draws the error border too, as the web field does.
    val shownInvalid = invalid || LocalMonarchFieldInvalid.current
    val shownSuccess = success && !shownInvalid
    return remember(enabled, shownInvalid, shownSuccess, focused, hovered) {
        MonarchFieldState(
            enabled = enabled,
            invalid = shownInvalid,
            success = shownSuccess,
            focused = focused,
            hovered = hovered,
        )
    }
}

/**
 * Whether the surrounding `MonarchFormField` has an error. The field sets it for its control, so a Monarch field
 * box inside it is drawn in the error state without the caller passing the error twice.
 */
internal val LocalMonarchFieldInvalid = compositionLocalOf { false }

/** Whether the field box shows the AI tint (the AI wash and border), as a suggested value in an autofill field does. */
internal val LocalMonarchFieldAiTint = compositionLocalOf { false }

/** An interaction source to share between the box and the text engine inside it. */
@Composable
internal fun rememberFieldInteractionSource(): MutableInteractionSource = remember { MutableInteractionSource() }

@Composable
private fun Slot(tint: Color, top: Boolean, content: @Composable () -> Unit) {
    Box(
        Modifier.heightIn(min = MonarchFieldIconSize),
        contentAlignment = if (top) Alignment.TopCenter else Alignment.Center,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides tint,
            LocalMonarchFieldGlyphSize provides MonarchFieldIconSize,
            content = content,
        )
    }
}

@Composable
private fun Affix(text: String, color: Color, style: TextStyle) {
    Text(
        text = text,
        style = style,
        color = color,
        maxLines = 1,
    )
}

/**
 * Draws the focus ring outside the border: [width] wide (the theme's focus ring width, 2dp or 3dp in high
 * contrast) after a gap of the same width, as `monarchFocusRing` draws it around other controls. A rounded shape
 * gets a concentric ring (each corner radius grows by the outset), so the gap is even all the way round.
 */
private fun Modifier.fieldRing(color: Color?, shape: Shape, width: Dp): Modifier {
    if (color == null) return this
    return drawBehind {
        val ring = width.toPx()
        // The stroke is centred half a ring beyond the gap.
        val outset = ring * GapAndHalfRing
        val grown = Size(size.width + outset * 2, size.height + outset * 2)
        val ringShape = if (shape is RoundedCornerShape) {
            RoundedCornerShape(
                topStart = shape.topStart.toPx(size, this) + outset,
                topEnd = shape.topEnd.toPx(size, this) + outset,
                bottomEnd = shape.bottomEnd.toPx(size, this) + outset,
                bottomStart = shape.bottomStart.toPx(size, this) + outset,
            )
        } else {
            shape
        }
        val outline = ringShape.createOutline(grown, layoutDirection, this)
        translate(left = -outset, top = -outset) {
            drawOutline(outline = outline, color = color, style = Stroke(width = ring))
        }
    }
}

/** The gap (one ring width) plus half the ring: where the centre of the ring's stroke sits. */
private const val GapAndHalfRing = 1.5f
