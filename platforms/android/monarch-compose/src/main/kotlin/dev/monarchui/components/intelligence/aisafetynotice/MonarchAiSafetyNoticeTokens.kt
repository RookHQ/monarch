package dev.monarchui.components.intelligence.aisafetynotice

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.components.collaboration.linkcard.MonarchLinkCardTokens
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchSurfaceColors
import dev.monarchui.internal.MonarchTonePalette
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens and the pure logic of MonarchAiSafetyNotice. */
internal object MonarchAiSafetyNoticeTokens {
    val HorizontalPadding: Dp = MonarchTokens.Space.space4.dp
    val VerticalPadding: Dp = MonarchTokens.Space.space3.dp
    val ContentGap: Dp = MonarchTokens.Space.space3.dp
    val TextGap: Dp = MonarchTokens.Space.space1.dp
    val ActionGap: Dp = MonarchTokens.Space.space2.dp

    /** A blocked request is a warning; a changed or shortened answer is information. */
    fun tone(kind: MonarchAiSafetyNoticeKind): MonarchTone = when (kind) {
        MonarchAiSafetyNoticeKind.Blocked -> MonarchTone.Warning
        MonarchAiSafetyNoticeKind.Modified, MonarchAiSafetyNoticeKind.Filtered -> MonarchTone.Info
    }

    /** The surface of the notice: the same colours as `MonarchBanner` in the tone of the kind. */
    fun colors(kind: MonarchAiSafetyNoticeKind, colors: MonarchColors): MonarchSurfaceColors {
        val palette = MonarchTonePalette.of(tone(kind), colors)
        return MonarchSurfaceColors(container = palette.subtle, content = palette.strong, border = palette.border)
    }

    /** The text, or `null` when it is missing or only white space. */
    fun present(text: String?): String? = text?.takeIf { it.isNotBlank() }

    /** The guidelines link to show: only http, https and mailto links; anything else is dropped. */
    fun safeUrl(url: String?): String? = url?.trim()?.takeIf { it.isNotEmpty() && MonarchLinkCardTokens.isSafe(it) }
}
