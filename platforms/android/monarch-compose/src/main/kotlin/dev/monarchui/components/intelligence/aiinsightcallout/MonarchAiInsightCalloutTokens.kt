package dev.monarchui.components.intelligence.aiinsightcallout

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.R
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchSurfaceColors
import dev.monarchui.internal.MonarchTonePalette
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens and pure logic of MonarchAiInsightCallout, built from primitives and roles only. */
internal object MonarchAiInsightCalloutTokens {
    /** The gap between the icon, the body and the dismiss button. */
    val Gap: Dp = MonarchTokens.Space.space3.dp

    /** The gap between the parts of the body. */
    val BodyGap: Dp = MonarchTokens.Space.space2.dp

    /** The gap between the title and the origin label, both ways when they wrap. */
    val HeaderGap: Dp = MonarchTokens.Space.space2.dp

    /** The padding inside the callout. */
    val Padding: Dp = MonarchTokens.Space.space3.dp

    /** The padding at the start, past the tone bar. */
    val StartPadding: Dp = MonarchTokens.Space.space4.dp

    /** The width of the tone bar on the start edge. */
    val BarWidth: Dp = MonarchTokens.Space.space1.dp

    /** The gap between the origin icon and its text. */
    val OriginGap: Dp = MonarchTokens.Space.space1.dp

    /** The padding at the start and end of the origin label. */
    val OriginHorizontalPadding: Dp = MonarchTokens.Space.space1_5.dp

    /** The padding above and below the origin label's text. */
    val OriginVerticalPadding: Dp = MonarchTokens.Space.px.dp

    /** The colours of the callout for a tone. */
    data class Surface(val fill: Color, val border: Color, val accent: Color)

    /** The tone's subtle fill and border, and its strong colour for the icon and the bar. */
    fun surface(tone: MonarchTone, colors: MonarchColors): Surface {
        val palette = MonarchTonePalette.of(tone, colors)
        return Surface(palette.subtle, palette.border, palette.strong)
    }

    /** The origin label: only a model-made insight uses the assistant colours. */
    fun origin(origin: MonarchAiInsightOrigin, colors: MonarchColors): MonarchSurfaceColors = when (origin) {
        MonarchAiInsightOrigin.Ai -> MonarchTonePalette.of(MonarchTone.Ai, colors).let {
            MonarchSurfaceColors(it.subtle, it.strong, it.border)
        }
        MonarchAiInsightOrigin.Rule -> MonarchSurfaceColors(colors.bgSurface, colors.textMuted, colors.borderDefault)
    }

    /** The built-in name of a tone, read before the title, or `null` for the neutral tone, which has none. */
    fun toneResource(tone: MonarchTone): Int? = when (tone) {
        MonarchTone.Neutral, MonarchTone.Accent -> null
        MonarchTone.Ai -> R.string.monarch_internal_tone_ai
        MonarchTone.Info -> R.string.monarch_aiinsightcallout_tone_info
        MonarchTone.Success -> R.string.monarch_aiinsightcallout_tone_success
        MonarchTone.Warning -> R.string.monarch_aiinsightcallout_tone_warning
        MonarchTone.Danger -> R.string.monarch_aiinsightcallout_tone_danger
    }

    /** Whether the title is read with a tone name first, so the tone is never colour alone. */
    fun namesTone(toneName: String?): Boolean = !toneName.isNullOrEmpty()
}
