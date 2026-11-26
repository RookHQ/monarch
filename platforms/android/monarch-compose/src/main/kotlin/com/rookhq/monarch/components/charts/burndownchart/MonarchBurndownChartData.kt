package com.rookhq.monarch.components.charts.burndownchart

import kotlin.math.floor

/** Where the sprint stands on the latest day with work left, against the ideal. */
internal enum class MonarchBurndownChartTrend { Ahead, Behind, OnTrack }

/**
 * The figures of a burndown worked out from its points. Pure.
 *
 * @property remaining the work left per day, `null` for a day not reached or a gap.
 * @property scope the scope per day, `null` where there is none.
 * @property ideal the ideal per day: from the start down to zero on the last day; `null` everywhere without a start.
 * @property changes the scope change per day: the difference from the day before, zero without one.
 * @property lastActual the latest day with work left, or -1 when the sprint has not started.
 * @property today the day of the today marker, or `null` for none.
 * @property hasScope whether any day has a scope.
 * @property added the work added to the scope since the start.
 * @property removed the work removed from the scope since the start.
 */
@Suppress("LongParameterList")
internal class MonarchBurndownChartData(
    val remaining: List<Double?>,
    val scope: List<Double?>,
    val ideal: List<Double?>,
    val changes: List<Double>,
    val lastActual: Int,
    val today: Int?,
    val hasScope: Boolean,
    val added: Double,
    val removed: Double,
) {
    /** The latest scope, or `null` without one. */
    val lastScope: Double? get() = scope.lastOrNull { it != null }

    /** Where the sprint stands on [lastActual], or `null` before it starts. */
    val trend: MonarchBurndownChartTrend?
        get() = difference?.let {
            when {
                it > 0.0 -> MonarchBurndownChartTrend.Behind
                it < 0.0 -> MonarchBurndownChartTrend.Ahead
                else -> MonarchBurndownChartTrend.OnTrack
            }
        }

    /**
     * The work left minus the ideal on [lastActual], rounded half up to two decimals as on the web so float noise
     * never reads as behind, or `null` before the sprint starts.
     */
    val difference: Double?
        get() {
            if (lastActual < 0) return null
            val left = remaining[lastActual] ?: return null
            val target = ideal[lastActual] ?: left
            return floor((left - target) * HUNDRED + HALF) / HUNDRED
        }

    companion object {
        private const val HUNDRED = 100.0
        private const val HALF = 0.5

        /**
         * Works out the figures of [points]. The ideal starts at [idealStart], else the first day's scope, else its
         * work left. The today marker is [todayIndex] when it is a day, else the latest day with work left when
         * later days exist; none when [showToday] is off.
         */
        fun of(
            points: List<MonarchBurndownChartPoint>,
            idealStart: Double?,
            todayIndex: Int?,
            showToday: Boolean,
        ): MonarchBurndownChartData {
            val n = points.size
            val remaining = points.map { it.remaining?.takeIf(Double::isFinite) }
            val scope = points.map { it.scope?.takeIf(Double::isFinite) }
            val start = idealStart?.takeIf(Double::isFinite) ?: scope.firstOrNull() ?: remaining.firstOrNull()
            val ideal = List(n) { i ->
                start?.let { if (n <= 1) it else it * (n - 1 - i) / (n - 1) }
            }
            val lastActual = remaining.indexOfLast { it != null }
            val today = when {
                !showToday -> null
                todayIndex != null -> todayIndex.takeIf { it in 0 until n }
                lastActual >= 0 && lastActual < n - 1 -> lastActual
                else -> null
            }
            val changes = List(n) { i ->
                val now = scope[i]
                val before = scope.getOrNull(i - 1)
                if (now != null && before != null) now - before else 0.0
            }
            return MonarchBurndownChartData(
                remaining = remaining,
                scope = scope,
                ideal = ideal,
                changes = changes,
                lastActual = lastActual,
                today = today,
                hasScope = scope.any { it != null },
                added = changes.filter { it > 0.0 }.sum(),
                removed = changes.filter { it < 0.0 }.sumOf { -it },
            )
        }
    }
}
