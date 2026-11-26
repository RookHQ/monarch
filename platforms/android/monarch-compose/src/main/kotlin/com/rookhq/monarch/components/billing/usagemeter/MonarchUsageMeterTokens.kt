package com.rookhq.monarch.components.billing.usagemeter

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** The level a metered item has reached. */
internal enum class MonarchUsageLevel { Normal, Warning, Over, Unlimited }

/** Component tokens and the pure rules of MonarchUsageMeter. */
internal object MonarchUsageMeterTokens {
    /** The default share of the limit at which an item is near the limit. */
    const val DEFAULT_WARNING_AT = 0.8

    val ItemGap: Dp = MonarchTokens.Space.space4.dp
    val LineGap: Dp = MonarchTokens.Space.space1.dp
    val HeaderGap: Dp = MonarchTokens.Space.space1.dp

    /** The limit, or `null` when there is none: missing, zero, negative or not finite. */
    fun limitOf(limit: Double?): Double? = limit?.takeIf { it.isFinite() && it > 0.0 }

    /** The level of [used] against [limit]; exactly at the limit is near it, not over it. */
    fun level(used: Double, limit: Double?, warningAt: Double): MonarchUsageLevel {
        val cap = limitOf(limit) ?: return MonarchUsageLevel.Unlimited
        return when {
            used > cap -> MonarchUsageLevel.Over
            used >= cap * warningAt -> MonarchUsageLevel.Warning
            else -> MonarchUsageLevel.Normal
        }
    }

    /**
     * The items whose level rose to near or over the limit since [previous] (the levels by item id before the
     * change), in display order. An item seen for the first time is not a change, so nothing is read when the
     * meter first appears.
     */
    fun crossings(
        previous: Map<String, MonarchUsageLevel>,
        items: List<MonarchUsageMeterItem>,
        warningAt: Double,
    ): List<Pair<MonarchUsageMeterItem, MonarchUsageLevel>> = items.mapNotNull { item ->
        val before = previous[item.id] ?: return@mapNotNull null
        val now = level(item.used, item.limit, warningAt)
        val rose = now.ordinal > before.ordinal && (now == MonarchUsageLevel.Warning || now == MonarchUsageLevel.Over)
        if (rose && before != MonarchUsageLevel.Unlimited) item to now else null
    }

    /** The filled share of the meter, from 0 to 1; above the limit the meter is full. */
    fun fraction(used: Double, limit: Double?): Float {
        val cap = limitOf(limit) ?: return 0f
        return (used / cap).coerceIn(0.0, 1.0).toFloat()
    }

    /** The meter colour: the accent, the warning colour near the limit and the danger colour over it. */
    fun meterColor(level: MonarchUsageLevel, colors: MonarchColors): Color = when (level) {
        MonarchUsageLevel.Warning -> colors.warningSolid
        MonarchUsageLevel.Over -> colors.dangerSolid
        MonarchUsageLevel.Normal, MonarchUsageLevel.Unlimited -> colors.accentSolid
    }

    /** The tone of the bar: neutral until the limit is near, then warning, then danger. */
    fun meterTone(level: MonarchUsageLevel): MonarchTone = when (level) {
        MonarchUsageLevel.Warning -> MonarchTone.Warning
        MonarchUsageLevel.Over -> MonarchTone.Danger
        MonarchUsageLevel.Normal, MonarchUsageLevel.Unlimited -> MonarchTone.Neutral
    }

    /** The colour of the status line: muted, or the strong colour of the level. */
    fun statusColor(level: MonarchUsageLevel, colors: MonarchColors): Color = when (level) {
        MonarchUsageLevel.Warning -> colors.warningStrong
        MonarchUsageLevel.Over -> colors.dangerStrong
        MonarchUsageLevel.Normal, MonarchUsageLevel.Unlimited -> colors.textMuted
    }

    fun trackColor(colors: MonarchColors): Color = colors.bgEmphasis
}
