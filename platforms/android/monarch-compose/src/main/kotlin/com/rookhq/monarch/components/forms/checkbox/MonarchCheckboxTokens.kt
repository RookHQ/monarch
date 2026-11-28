package com.rookhq.monarch.components.forms.checkbox

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** The colours of the box of a checkbox in one state. */
internal data class MonarchCheckboxColors(val container: Color, val border: Color, val mark: Color)

/** Component tokens of MonarchCheckbox, from `checkbox.css`. */
internal object MonarchCheckboxTokens {
    /** The corner radius of the box: the web's `radius-xs`, 4dp. */
    val Radius: Dp = MonarchTokens.Radius.xs.dp

    /** The stroke of the check mark and the dash: 1.5dp, crisp at 16dp without looking bold. */
    val MarkStroke: Dp = MonarchTokens.Space.space0_5.dp * MarkStrokeFraction

    /** The check on the 16-unit grid of the box: a short stroke down, a long one up, optically centred. */
    val CheckPoints: List<Offset> = listOf(Offset(4.25f, 8.25f), Offset(6.75f, 10.75f), Offset(11.75f, 5.5f))

    /** The dash of the partly-on box on the same grid. */
    val DashPoints: List<Offset> = listOf(Offset(4.5f, 8f), Offset(11.5f, 8f))

    /** The size of the grid the mark points are given on. */
    const val GRID: Float = 16f

    /**
     * The colours of the box. Off: the surface with the input border. On or indeterminate: the accent fill and
     * border with the on-accent mark. Invalid: the danger border, over either fill, as on the web. Pressed (the
     * web has no hover on touch): the muted fill with the subtle-text border when off, the pressed accent
     * when on. Disabled (never opacity): the subtle fill with the default border when off, the emphasis fill with a
     * disabled-text mark when on.
     */
    fun colors(
        on: Boolean,
        invalid: Boolean,
        pressed: Boolean,
        colors: MonarchColors,
        enabled: Boolean = true,
    ): MonarchCheckboxColors {
        if (!enabled) {
            return if (on) {
                MonarchCheckboxColors(colors.bgEmphasis, colors.bgEmphasis, colors.textDisabled)
            } else {
                MonarchCheckboxColors(colors.bgSubtle, colors.borderDefault, colors.textDisabled)
            }
        }
        val container = when {
            on && pressed -> colors.accentSolidActive
            on -> colors.accentStrong
            pressed -> colors.bgMuted
            else -> colors.bgSurface
        }
        val border = when {
            invalid -> colors.dangerStrong
            on && pressed -> colors.accentSolidActive
            on -> colors.accentStrong
            pressed -> colors.textSubtle
            else -> colors.borderInput
        }
        return MonarchCheckboxColors(container = container, border = border, mark = colors.accentOnStrong)
    }
}

/** 2dp times 0.75: the 1.5dp mark stroke, from a spacing token. */
private const val MarkStrokeFraction = 0.75f
