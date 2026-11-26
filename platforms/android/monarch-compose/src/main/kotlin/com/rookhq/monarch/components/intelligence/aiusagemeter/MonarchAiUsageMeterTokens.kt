package com.rookhq.monarch.components.intelligence.aiusagemeter

import com.rookhq.monarch.components.billing.usagemeter.MonarchUsageLevel
import com.rookhq.monarch.components.billing.usagemeter.MonarchUsageMeterTokens

/** The pure rules of MonarchAiUsageMeter, on top of those of MonarchUsageMeter. */
internal object MonarchAiUsageMeterTokens {
    /** The amount used, never below zero. */
    fun used(scope: MonarchAiUsageScope): Double = scope.used.coerceAtLeast(0.0)

    /** The limit of a scope, or `null` when it has none. */
    fun limit(scope: MonarchAiUsageScope): Double? = MonarchUsageMeterTokens.limitOf(scope.limit)

    /** The level of a scope, by the rules of `MonarchUsageMeter`: exactly at the limit is near it. */
    fun level(scope: MonarchAiUsageScope, warningAt: Double): MonarchUsageLevel =
        MonarchUsageMeterTokens.level(used(scope), scope.limit, warningAt)

    /** The count the unit agrees with: the limit, or the amount when there is no limit. */
    fun unitCount(scope: MonarchAiUsageScope): Double = limit(scope) ?: used(scope)

    /** The plural quantity of a count: whole counts are themselves; fractions take the other form. */
    fun quantity(count: Double): Int = when {
        count == 1.0 -> 1
        count % 1.0 != 0.0 -> FRACTION_QUANTITY
        count >= Int.MAX_VALUE -> Int.MAX_VALUE
        else -> count.toInt()
    }

    /** The scope whose level is the most serious, first in order on a tie, or `null` when none is near its limit. */
    fun worst(scopes: List<MonarchAiUsageScope>, warningAt: Double): MonarchAiUsageScope? {
        var worst: MonarchAiUsageScope? = null
        var rank = 0
        scopes.forEach { scope ->
            val r = rank(level(scope, warningAt))
            if (r > rank) {
                rank = r
                worst = scope
            }
        }
        return worst
    }

    private fun rank(level: MonarchUsageLevel): Int = when (level) {
        MonarchUsageLevel.Over -> 2
        MonarchUsageLevel.Warning -> 1
        MonarchUsageLevel.Normal, MonarchUsageLevel.Unlimited -> 0
    }

    // English and most languages use the "other" form for a fractional count; 2 selects it there.
    private const val FRACTION_QUANTITY = 2
}
