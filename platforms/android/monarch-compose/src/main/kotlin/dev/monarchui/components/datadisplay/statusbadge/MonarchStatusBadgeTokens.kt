package dev.monarchui.components.datadisplay.statusbadge

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.R
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.foundation.MonarchTypography
import dev.monarchui.internal.MonarchSurfaceColors
import dev.monarchui.internal.MonarchToneEmphasis
import dev.monarchui.internal.MonarchTonePalette
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens
import java.util.Locale

/** A built-in status: its tone and the resource of its label. */
internal class BuiltInStatus(val tone: MonarchTone, @StringRes val label: Int)

/** Component tokens and rules of MonarchStatusBadge, built from primitives and roles only. */
internal object MonarchStatusBadgeTokens {
    val HorizontalPadding: Dp = MonarchTokens.Space.space2.dp
    val VerticalPadding: Dp = MonarchTokens.Space.space0_5.dp
    val Gap: Dp = MonarchTokens.Space.space1_5.dp

    /** The diameter of the dot of the dot variant at the default font size. */
    val DotSize: Dp = MonarchTokens.Space.space2.dp

    const val FONT_WEIGHT: Int = MonarchTokens.FontWeight.medium

    /** The key used for a `null` status. */
    const val UNKNOWN: String = "unknown"

    /** The statuses Monarch ships, service health first, then the generic set, as on the web. */
    val BuiltIn: Map<String, BuiltInStatus> = mapOf(
        "operational" to BuiltInStatus(MonarchTone.Success, R.string.monarch_statusbadge_operational),
        "degraded" to BuiltInStatus(MonarchTone.Warning, R.string.monarch_statusbadge_degraded),
        "partial" to BuiltInStatus(MonarchTone.Warning, R.string.monarch_statusbadge_partial),
        "down" to BuiltInStatus(MonarchTone.Danger, R.string.monarch_statusbadge_down),
        "maintenance" to BuiltInStatus(MonarchTone.Info, R.string.monarch_statusbadge_maintenance),
        UNKNOWN to BuiltInStatus(MonarchTone.Neutral, R.string.monarch_statusbadge_unknown),
        "success" to BuiltInStatus(MonarchTone.Success, R.string.monarch_statusbadge_success),
        "warning" to BuiltInStatus(MonarchTone.Warning, R.string.monarch_statusbadge_warning),
        "danger" to BuiltInStatus(MonarchTone.Danger, R.string.monarch_statusbadge_danger),
        "info" to BuiltInStatus(MonarchTone.Info, R.string.monarch_statusbadge_info),
        "neutral" to BuiltInStatus(MonarchTone.Neutral, R.string.monarch_statusbadge_neutral),
    )

    /**
     * A readable label for a key that no registry knows: "pending_delete"
     * becomes "Pending delete". `null` for a key with no letters, which then
     * reads as the built-in "Unknown".
     */
    fun formatLabel(key: String): String? {
        val words = key.replace(Regex("[_-]+"), " ").trim().lowercase(Locale.ROOT)
        return words.takeIf { it.isNotEmpty() }?.replaceFirstChar { it.titlecase(Locale.getDefault()) }
    }

    /** The fill, content and border of the badge. The dot variant has no fill and default text. */
    fun colors(tone: MonarchTone, variant: MonarchStatusBadgeVariant, colors: MonarchColors): MonarchSurfaceColors {
        val palette = MonarchTonePalette.of(tone, colors)
        return when (variant) {
            MonarchStatusBadgeVariant.Subtle -> palette.surface(MonarchToneEmphasis.Subtle)
            MonarchStatusBadgeVariant.Outline -> palette.surface(MonarchToneEmphasis.Outline)
            MonarchStatusBadgeVariant.Solid -> palette.surface(MonarchToneEmphasis.Solid)
            MonarchStatusBadgeVariant.Dot -> MonarchSurfaceColors(Color.Transparent, colors.textDefault, null)
        }
    }

    /** The colour of the dot: the solid colour of the tone; the neutral tone uses the subtle text colour. */
    fun dotColor(tone: MonarchTone, colors: MonarchColors): Color =
        if (tone == MonarchTone.Neutral) colors.textSubtle else MonarchTonePalette.of(tone, colors).solid

    /** The text style: caption for `Sm`, label for `Md`, both at medium weight. */
    fun textStyle(size: MonarchStatusBadgeSize, typography: MonarchTypography): TextStyle {
        val base = if (size == MonarchStatusBadgeSize.Sm) typography.caption else typography.label
        return base.copy(fontWeight = FontWeight(FONT_WEIGHT))
    }
}
