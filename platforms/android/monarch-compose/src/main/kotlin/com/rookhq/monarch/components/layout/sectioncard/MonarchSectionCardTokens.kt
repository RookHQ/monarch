package com.rookhq.monarch.components.layout.sectioncard

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.R
import com.rookhq.monarch.accessibility.MonarchTouchTarget
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchTonePalette
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens of MonarchSectionCard, built from primitives and roles only. */
internal object MonarchSectionCardTokens {
    /** The width of the card's border and of the line above the footer. */
    val BorderWidth: Dp = MonarchTokens.Space.px.dp

    /** The space inside the card around the header, the body and the footer. */
    val Inset: Dp = MonarchTokens.Space.space4.dp

    /** The space between the header and the body. */
    val HeaderGap: Dp = MonarchTokens.Space.space3.dp

    /** The space between the icon, the header and the toggle icon. */
    val IconGap: Dp = MonarchTokens.Space.space2.dp

    /** The space between the parts of the footer, and above and below it. */
    val FooterGap: Dp = MonarchTokens.Space.space3.dp

    /** A collapsible header is at least a touch target high. */
    val ToggleMinHeight: Dp = MonarchTouchTarget.Minimum

    /** The number of skeleton rows while loading, as on the web. */
    const val LOADING_ROWS: Int = 3

    /** The turn of the toggle icon while the body is shown. */
    const val EXPANDED_ROTATION: Float = 180f

    /** The padding of the body: none when [flush], the inset at the sides and bottom otherwise. */
    fun bodyPadding(flush: Boolean): PaddingValues = if (flush) {
        PaddingValues(MonarchTokens.Space.space0.dp)
    } else {
        PaddingValues(start = Inset, end = Inset, bottom = Inset)
    }

    /** The border of the card: the default border for the neutral tone, the tone's border otherwise. */
    fun border(tone: MonarchTone, colors: MonarchColors): Color =
        if (tone == MonarchTone.Neutral) colors.borderDefault else MonarchTonePalette.of(tone, colors).border

    /** The colour of the leading icon: muted for the neutral tone, the tone's strong colour otherwise. */
    fun iconColor(tone: MonarchTone, colors: MonarchColors): Color =
        if (tone == MonarchTone.Neutral) colors.textMuted else MonarchTonePalette.of(tone, colors).strong

    /** The fill of the card. */
    fun container(colors: MonarchColors): Color = colors.bgSurface

    /** The built-in spoken name of [tone], or `null` for the neutral tone, which is not spoken. */
    fun toneName(tone: MonarchTone): Int? = when (tone) {
        MonarchTone.Neutral, MonarchTone.Accent -> null
        MonarchTone.Ai -> R.string.monarch_internal_tone_ai
        MonarchTone.Info -> R.string.monarch_sectioncard_tone_info
        MonarchTone.Success -> R.string.monarch_sectioncard_tone_success
        MonarchTone.Warning -> R.string.monarch_sectioncard_tone_warning
        MonarchTone.Danger -> R.string.monarch_sectioncard_tone_danger
    }

    /** The state TalkBack reads on the title: the parts that apply, in order, separated by commas. */
    fun spokenState(parts: List<String?>, separator: String): String? =
        parts.filterNotNull().filter { it.isNotEmpty() }.takeIf { it.isNotEmpty() }?.joinToString(separator)
}
