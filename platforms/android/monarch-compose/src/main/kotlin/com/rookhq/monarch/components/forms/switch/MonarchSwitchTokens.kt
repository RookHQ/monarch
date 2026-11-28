package com.rookhq.monarch.components.forms.switch

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.lerp
import com.rookhq.monarch.foundation.MonarchControlMetrics
import com.rookhq.monarch.theme.MonarchColors

/** The colours of a switch in one position. */
internal data class MonarchSwitchColors(val track: Color, val border: Color, val thumb: Color)

/** Component tokens of MonarchSwitch: the Material 52 by 32dp geometry and the web's colours. */
internal object MonarchSwitchTokens {
    /** The thumb is 16dp off, 24dp on and 28dp while pressed, as in Material 3. */
    /** How much of the accent a disabled switch that is on keeps. */
    private const val DisabledStrength = 0.5f
    private val ThumbOff: Dp = MonarchControlMetrics.SwitchThumbOff
    private val ThumbOn: Dp = MonarchControlMetrics.SwitchThumbOn
    private val ThumbPressed: Dp = MonarchControlMetrics.SwitchThumbPressed

    /**
     * On: the accent track and border with a white thumb in every theme, as on iOS: the lighter of the surface
     * and the default text, so white in the light themes and the near-white text colour in the dark ones (the
     * on-accent colour is dark on the pink dark-theme track). Off: the muted track, input border, a subtle thumb.
     * Disabled: on is the accent track at half strength (blended into the surface, not an alpha) with a surface
     * thumb, as on the web; off is the muted track with the default border and a disabled-text thumb.
     */
    fun colors(checked: Boolean, colors: MonarchColors, enabled: Boolean = true): MonarchSwitchColors = when {
        !enabled && checked -> {
            val faded = lerp(colors.bgSurface, colors.accentStrong, DisabledStrength)
            MonarchSwitchColors(faded, faded, colors.bgSurface)
        }
        !enabled -> MonarchSwitchColors(colors.bgMuted, colors.borderDefault, colors.textDisabled)
        checked -> MonarchSwitchColors(colors.accentStrong, colors.accentStrong, onThumb(colors))
        else -> MonarchSwitchColors(colors.bgEmphasis, colors.borderInput, colors.textSubtle)
    }

    /** The thumb of a switch that is on: the lightest neutral of the theme, white or near white. */
    fun onThumb(colors: MonarchColors): Color =
        if (colors.textDefault.luminance() > colors.bgSurface.luminance()) colors.textDefault else colors.bgSurface

    /** The thumb diameter at [progress] (0 off, 1 on), with [grow] (0 to 1) while pressed. */
    fun thumbDiameter(progress: Float, grow: Float): Dp {
        val rest = lerp(ThumbOff.value, ThumbOn.value, progress)
        return Dp(lerp(rest, ThumbPressed.value, grow))
    }
}
