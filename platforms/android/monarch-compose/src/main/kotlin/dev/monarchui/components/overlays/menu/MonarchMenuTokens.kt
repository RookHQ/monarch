package dev.monarchui.components.overlays.menu

import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.sp
import dev.monarchui.foundation.MonarchTypography
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** The colours of one menu row in its current state. */
@Immutable
internal data class MonarchMenuItemColors(val text: Color, val icon: Color, val highlight: Color)

/**
 * Component tokens of the Monarch menu, from the web's `menu.css`: a panel
 * of 192 to 352dp with 12dp corners, a subtle hairline and 4dp around the
 * rows; rows with a 40dp highlight with 6dp corners inside a 48dp touch
 * target, 8dp of padding, 20dp muted icons, 14 text, an accent check for the
 * chosen row and the strong danger colour for a destructive one.
 */
internal object MonarchMenuTokens {
    val MinWidth: Dp = (MonarchTokens.Space.space24 * 2).dp
    val MaxWidth: Dp = (MonarchTokens.Space.space24 * 4 - MonarchTokens.Space.space8).dp

    /** The space between the panel edge and a row highlight. */
    val Inset: Dp = MonarchTokens.Space.space1.dp

    /** The touch target of a row; the highlight is this less the inset above and below. */
    val RowTarget: Dp = MonarchTokens.Control.xl.dp

    /** The padding inside the highlight, at the sides. */
    val RowPadding: Dp = MonarchTokens.Space.space2.dp

    /** Between the icon, the text, the shortcut and the check. */
    val RowGap: Dp = MonarchTokens.Space.space3.dp

    /** The check of a chosen row. */
    val CheckSize: Dp = MonarchTokens.Space.space4.dp

    /** The vertical padding that Material puts around the rows, removed so the inset is 4dp. */
    val MaterialPadding: Dp = MonarchTokens.Space.space2.dp

    /** A soft shadow under the panel in the light themes; a dark theme uses the surface step and the border. */
    val ShadowElevation: Dp = MonarchTokens.Space.space1_5.dp

    /** A group label: 8dp above, 4dp below, aligned with the row text. */
    val LabelTop: Dp = MonarchTokens.Space.space2.dp
    val LabelBottom: Dp = MonarchTokens.Space.space1.dp

    /** The text of a row: 14 regular, snug. */
    fun textStyle(typography: MonarchTypography): TextStyle = typography.body.copy(
        fontSize = MonarchTokens.FontSize.sm.sp,
        lineHeight = MonarchTokens.LineHeight.snug.em,
    )

    /** A shortcut and a group label: 12, wide tracking for the shortcut. */
    fun shortcutStyle(typography: MonarchTypography): TextStyle =
        typography.caption.copy(letterSpacing = MonarchTokens.LetterSpacing.wide.em)

    /**
     * The colours of a row. Highlighted (pressed, hovered or focused) rows take
     * one surface step, a destructive row the danger tint.
     */
    fun itemColors(
        colors: MonarchColors,
        enabled: Boolean,
        destructive: Boolean,
        highlighted: Boolean,
    ): MonarchMenuItemColors {
        val text = when {
            !enabled -> colors.textDisabled
            destructive -> colors.dangerStrong
            else -> colors.textDefault
        }
        val icon = when {
            !enabled -> colors.textDisabled
            destructive -> colors.dangerStrong
            else -> colors.textMuted
        }
        val highlight = when {
            !enabled || !highlighted -> Color.Transparent
            destructive -> colors.dangerSubtle
            else -> colors.bgMuted
        }
        return MonarchMenuItemColors(text, icon, highlight)
    }

    /**
     * The panel border and the dividers: subtle in a light theme, the default
     * border in a dark one (where the subtle border nearly matches the raised
     * surface), strong in the high-contrast themes.
     */
    fun border(colors: MonarchColors, dark: Boolean, highContrast: Boolean): Color = when {
        highContrast -> colors.borderStrong
        dark -> colors.borderDefault
        else -> colors.borderSubtle
    }
}

/**
 * Removes [trim] above and below the content, which Material's menu pads by
 * a fixed amount. The content is measured with that much more room and
 * placed that much higher; the panel clips the padding that is left over.
 */
internal fun Modifier.monarchTrimVertical(trim: Dp): Modifier = layout { measurable, constraints ->
    val amount = trim.roundToPx()
    val placeable = measurable.measure(constraints.offset(vertical = amount * 2))
    val height = (placeable.height - amount * 2).coerceAtLeast(0)
    layout(placeable.width, height) { placeable.place(0, -amount) }
}
