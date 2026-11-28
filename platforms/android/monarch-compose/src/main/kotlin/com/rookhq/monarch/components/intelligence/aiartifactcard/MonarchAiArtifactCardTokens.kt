package com.rookhq.monarch.components.intelligence.aiartifactcard

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiArtifactKind
import com.rookhq.monarch.R
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** The surface of the card in one state. */
@Immutable
internal data class MonarchAiArtifactCardColors(
    val container: Color,
    val border: Color,
    val borderWidth: Dp,
    val title: Color,
    val meta: Color,
    val action: Color,
)

/** Component tokens and the pure logic of MonarchAiArtifactCard. */
internal object MonarchAiArtifactCardTokens {
    val Padding: Dp = MonarchTokens.Space.space3.dp
    val Gap: Dp = MonarchTokens.Space.space3.dp
    val TextGap: Dp = MonarchTokens.Space.space0_5.dp
    val ActionGap: Dp = MonarchTokens.Space.space1.dp
    val IconTile: Dp = MonarchTokens.Space.space10.dp
    val IconSize: Dp = MonarchTokens.Space.space5.dp
    val MinHeight: Dp = MonarchTokens.Space.space12.dp
    private val BorderWidth: Dp = MonarchTokens.Space.px.dp
    private val ActiveBorderWidth: Dp = MonarchTokens.Space.space0_5.dp

    /**
     * The colours: a surface card with the default border at rest, the subtle
     * AI fill with a 2dp AI border while active, and the disabled roles when
     * disabled.
     */
    fun colors(active: Boolean, enabled: Boolean, colors: MonarchColors): MonarchAiArtifactCardColors = when {
        !enabled -> MonarchAiArtifactCardColors(
            container = colors.bgSubtle,
            border = colors.borderSubtle,
            borderWidth = BorderWidth,
            title = colors.textDisabled,
            meta = colors.textDisabled,
            action = colors.textDisabled,
        )
        active -> MonarchAiArtifactCardColors(
            container = colors.aiSubtle,
            border = colors.aiBorder,
            borderWidth = ActiveBorderWidth,
            title = colors.textDefault,
            meta = colors.textMuted,
            action = colors.aiStrong,
        )
        else -> MonarchAiArtifactCardColors(
            container = colors.bgSurface,
            border = colors.borderDefault,
            borderWidth = BorderWidth,
            title = colors.textDefault,
            meta = colors.textMuted,
            action = colors.aiStrong,
        )
    }

    /** The icon tile: the subtle AI fill with the strong AI colour for the icon. */
    fun iconTile(colors: MonarchColors): Pair<Color, Color> = colors.aiSubtle to colors.aiStrong

    /** The built-in name of each kind. */
    fun kindResource(kind: MonarchAiArtifactKind): Int = when (kind) {
        MonarchAiArtifactKind.Document -> R.string.monarch_aiartifactcard_document
        MonarchAiArtifactKind.Code -> R.string.monarch_aiartifactcard_code
        MonarchAiArtifactKind.Table -> R.string.monarch_aiartifactcard_table
        MonarchAiArtifactKind.Chart -> R.string.monarch_aiartifactcard_chart
        MonarchAiArtifactKind.Image -> R.string.monarch_aiartifactcard_image
        MonarchAiArtifactKind.Html -> R.string.monarch_aiartifactcard_html
    }
}
