package com.rookhq.monarch.components.navigation.link

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.internal.MonarchControlState
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/**
 * How a link looks in one state.
 *
 * @property color the text and icon colour.
 * @property underlined whether the text is underlined.
 * @property fontWeight the weight, or `null` to keep the weight of the surrounding text.
 */
@Immutable
internal data class MonarchLinkAppearance(val color: Color, val underlined: Boolean, val fontWeight: FontWeight?) {
    /** The appearance as a span style, for a link inside an annotated string. */
    fun toSpanStyle(): SpanStyle = SpanStyle(
        color = color,
        fontWeight = fontWeight,
        textDecoration = if (underlined) TextDecoration.Underline else TextDecoration.None,
    )
}

/** Component tokens of MonarchLink, built from primitives and roles only. */
internal object MonarchLinkTokens {
    val ContentGap: Dp = MonarchTokens.Space.space1.dp

    /**
     * The look of a link. Pressed, hovered and focused links are always
     * underlined, so the state does not rely on colour. In the high-contrast
     * themes every variant is underlined at rest too, so a link is never told
     * from text by colour alone.
     */
    fun appearance(
        variant: MonarchLinkVariant,
        state: MonarchControlState,
        highContrast: Boolean,
        colors: MonarchColors,
    ): MonarchLinkAppearance {
        val active = state.enabled && (state.pressed || state.hovered || state.focused)
        val color = when {
            !state.enabled -> colors.textDisabled
            variant == MonarchLinkVariant.Subtle -> if (active) colors.textDefault else colors.textMuted
            else -> colors.textLink
        }
        val underlined = variant == MonarchLinkVariant.Inline || active || highContrast
        val weight = if (variant == MonarchLinkVariant.Standalone) FontWeight(MonarchTokens.FontWeight.medium) else null
        return MonarchLinkAppearance(color = color, underlined = underlined, fontWeight = weight)
    }
}
