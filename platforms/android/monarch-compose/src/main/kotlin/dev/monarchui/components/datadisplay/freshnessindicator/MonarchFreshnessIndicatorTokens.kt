package dev.monarchui.components.datadisplay.freshnessindicator

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchRelativeTime
import dev.monarchui.internal.MonarchSurfaceColors
import dev.monarchui.internal.MonarchTonePalette
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens
import java.util.Locale

/** Component tokens and rules of MonarchFreshnessIndicator, built from primitives and roles only. */
internal object MonarchFreshnessIndicatorTokens {
    val DotSize: Dp = MonarchTokens.Space.space2.dp
    val DotGap: Dp = MonarchTokens.Space.space2.dp
    val ColumnGap: Dp = MonarchTokens.Space.space2.dp
    val RowGap: Dp = MonarchTokens.Space.space1.dp


    /** The middle dot between the parts of the line. Decorative: TalkBack hears commas. */
    const val SEPARATOR: String = " · "

    /**
     * The status worked out from the age of [updatedAt] at [now]: `Never`
     * without a time, `Expired` past [expiredAfter], `Stale` past
     * [staleAfter], else `Fresh`. A `null` limit is never reached.
     */
    fun status(updatedAt: Long?, now: Long, staleAfter: Long?, expiredAfter: Long?): MonarchFreshnessStatus {
        if (updatedAt == null) return MonarchFreshnessStatus.Never
        val age = now - updatedAt
        return when {
            expiredAfter != null && age > expiredAfter -> MonarchFreshnessStatus.Expired
            staleAfter != null && age > staleAfter -> MonarchFreshnessStatus.Stale
            else -> MonarchFreshnessStatus.Fresh
        }
    }

    /** Whether the line shows the time: there is one and the status is not `Never` or `Checking`. */
    fun showsTime(status: MonarchFreshnessStatus, updatedAt: Long?): Boolean =
        updatedAt != null && status != MonarchFreshnessStatus.Never && status != MonarchFreshnessStatus.Checking

    /** The tone of a status. */
    fun tone(status: MonarchFreshnessStatus): MonarchTone = when (status) {
        MonarchFreshnessStatus.Fresh -> MonarchTone.Success
        MonarchFreshnessStatus.Stale -> MonarchTone.Warning
        MonarchFreshnessStatus.Expired, MonarchFreshnessStatus.Error -> MonarchTone.Danger
        MonarchFreshnessStatus.Checking -> MonarchTone.Info
        MonarchFreshnessStatus.Never -> MonarchTone.Neutral
    }

    /** The dot takes the solid colour of the tone; the neutral tone uses the subtle text colour. */
    fun dotColor(status: MonarchFreshnessStatus, colors: MonarchColors): Color {
        val tone = tone(status)
        return if (tone == MonarchTone.Neutral) colors.textSubtle else MonarchTonePalette.of(tone, colors).solid
    }

    /** The colour of the flag or the error after the time: the strong colour of the tone. */
    fun flagColor(status: MonarchFreshnessStatus, colors: MonarchColors): Color =
        MonarchTonePalette.of(tone(status), colors).strong

    /** The banner has the subtle background and border; the inline form neither. Text is muted in both. */
    fun colors(variant: MonarchFreshnessIndicatorVariant, colors: MonarchColors): MonarchSurfaceColors =
        when (variant) {
            MonarchFreshnessIndicatorVariant.Inline -> MonarchSurfaceColors(Color.Transparent, colors.textMuted, null)
            MonarchFreshnessIndicatorVariant.Banner ->
                MonarchSurfaceColors(colors.bgSubtle, colors.textMuted, colors.borderSubtle)
        }

    /** The padding of the banner; the inline form has none. */
    fun padding(variant: MonarchFreshnessIndicatorVariant): PaddingValues = when (variant) {
        MonarchFreshnessIndicatorVariant.Inline -> PaddingValues(MonarchTokens.Space.space0.dp)
        MonarchFreshnessIndicatorVariant.Banner ->
            PaddingValues(horizontal = MonarchTokens.Space.space3.dp, vertical = MonarchTokens.Space.space1.dp)
    }

    /** [time] relative to [now] by the shared Monarch rule (the web rule): "30 seconds ago", "5 minutes ago". */
    fun relativeTime(time: Long, now: Long, locale: Locale = Locale.getDefault()): String =
        MonarchRelativeTime.format(time, now, locale)
}
