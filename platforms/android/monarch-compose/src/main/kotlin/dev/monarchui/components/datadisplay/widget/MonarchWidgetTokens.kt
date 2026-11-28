package dev.monarchui.components.datadisplay.widget

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.internal.MonarchRelativeTime
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens
import java.util.Locale

/** Component tokens and logic of MonarchWidget, built from primitives and roles only. */
internal object MonarchWidgetTokens {
    val SectionGap: Dp = MonarchTokens.Space.space3.dp
    val HeaderGap: Dp = MonarchTokens.Space.space2.dp
    val TitleGap: Dp = MonarchTokens.Space.space1.dp
    val SkeletonGap: Dp = MonarchTokens.Space.space2.dp
    val ContentPadding: PaddingValues = PaddingValues(MonarchTokens.Space.space4.dp)

    /** The widths of the three loading lines, as fractions of the body. */
    val SkeletonWidths: List<Float> = listOf(1f, 0.8f, 0.6f)

    /** From this font scale on, the header controls move under the title instead of squeezing it. */
    const val STACKED_HEADER_FONT_SCALE: Float = 1.5f

    fun stackedHeader(fontScale: Float): Boolean = fontScale >= STACKED_HEADER_FONT_SCALE

    /** Whether the footer is shown: not while loading, and only with something to say. */
    fun showsFooter(state: MonarchWidgetState, updatedAt: Long?, footer: String?): Boolean =
        state != MonarchWidgetState.Loading && (updatedAt != null || !footer.isNullOrEmpty())

    /** [time] relative to [now] by the shared Monarch rule (the web rule): "30 seconds ago", "5 minutes ago". */
    fun relativeTime(time: Long, now: Long, locale: Locale = Locale.getDefault()): String =
        MonarchRelativeTime.format(time, now, locale)

    fun titleColor(colors: MonarchColors): Color = colors.textDefault

    fun mutedColor(colors: MonarchColors): Color = colors.textMuted
}
