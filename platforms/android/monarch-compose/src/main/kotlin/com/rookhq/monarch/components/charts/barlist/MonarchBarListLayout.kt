package com.rookhq.monarch.components.charts.barlist

import kotlin.math.max

/** The pure ordering and scaling rules of MonarchBarList. */
internal object MonarchBarListLayout {
    /** A value that is `null` or not finite is missing. */
    fun valueOf(item: MonarchBarListItem): Double? = item.value?.takeIf { it.isFinite() }

    /** The rows in [sort] order. Equal values keep their given order; missing values go last. */
    fun sorted(items: List<MonarchBarListItem>, sort: MonarchBarListSort): List<MonarchBarListItem> {
        if (sort == MonarchBarListSort.None) return items
        val (present, missing) = items.partition { valueOf(it) != null }
        val ordered = when (sort) {
            MonarchBarListSort.Descending -> present.sortedByDescending { valueOf(it) }
            else -> present.sortedBy { valueOf(it) }
        }
        return ordered + missing
    }

    /** The value of a full bar: [maximum], or the largest value of all items (never below zero). */
    fun scale(items: List<MonarchBarListItem>, maximum: Double?): Double =
        maximum ?: max(0.0, items.mapNotNull { valueOf(it) }.maxOrNull() ?: 0.0)

    /** The share of a full bar, from 0 to 1. Missing and negative values, and an empty scale, draw no bar. */
    fun fraction(value: Double?, scale: Double): Float {
        if (value == null || !value.isFinite() || scale <= 0.0) return 0f
        return (value / scale).coerceIn(0.0, 1.0).toFloat()
    }
}
