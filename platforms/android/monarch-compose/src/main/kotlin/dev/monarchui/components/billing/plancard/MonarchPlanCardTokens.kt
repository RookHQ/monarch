package dev.monarchui.components.billing.plancard

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.components.datadisplay.badge.MonarchBadgeVariant
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens of MonarchPlanCard, built from primitives and roles only. */
internal object MonarchPlanCardTokens {
    /** The accent border of a highlighted card: a hairline, so the accent and the shadow carry the emphasis. */
    val HighlightBorderWidth: Dp = MonarchTokens.Space.px.dp
    val SectionGap: Dp = MonarchTokens.Space.space4.dp
    val LineGap: Dp = MonarchTokens.Space.space1.dp
    val BadgeGap: Dp = MonarchTokens.Space.space2.dp
    val FeatureGap: Dp = MonarchTokens.Space.space2.dp
    val MarkGap: Dp = MonarchTokens.Space.space2.dp
    val MarkWidth: Dp = MonarchTokens.Space.space4.dp

    fun highlightBorder(colors: MonarchColors): Color = colors.accentBorder

    /** The badge is subtle; on a highlighted card it takes the accent tone, as on the web. */
    val BadgeVariant: MonarchBadgeVariant = MonarchBadgeVariant.Subtle

    /** The tone of the badge beside the name: the accent when the card is highlighted. */
    fun badgeTone(highlighted: Boolean): MonarchTone = if (highlighted) MonarchTone.Accent else MonarchTone.Neutral

    /** Included features in the default text colour, excluded ones muted. */
    fun featureColor(included: Boolean, colors: MonarchColors): Color =
        if (included) colors.textDefault else colors.textMuted

    fun mutedColor(colors: MonarchColors): Color = colors.textMuted
}
