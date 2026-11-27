package com.rookhq.monarch.components.datadisplay.leaderboard

import com.rookhq.monarch.components.charts.barlist.MonarchBarListSort
import kotlin.math.abs

/** An entry with its rank. */
internal data class MonarchLeaderboardRanked(val entry: MonarchLeaderboardEntry, val rank: Int)

/** The rules of MonarchLeaderboard: ranks, the rows shown and the bars. */
internal object MonarchLeaderboardLogic {
    /** The ranks that the top highlight marks. */
    const val TOP_RANKS: Int = 3

    /**
     * Ranks [entries] as the web's rankEntries does: sorted by [sort], equal
     * values share a rank and the next rank skips (1, 1, 3).
     */
    fun rank(entries: List<MonarchLeaderboardEntry>, sort: MonarchBarListSort): List<MonarchLeaderboardRanked> {
        val ordered = when (sort) {
            MonarchBarListSort.Descending -> entries.sortedByDescending { it.value }
            MonarchBarListSort.Ascending -> entries.sortedBy { it.value }
            MonarchBarListSort.None -> entries
        }
        val ranked = mutableListOf<MonarchLeaderboardRanked>()
        ordered.forEachIndexed { index, entry ->
            val previous = ranked.lastOrNull()
            val rank = if (previous != null && previous.entry.value == entry.value) previous.rank else index + 1
            ranked += MonarchLeaderboardRanked(entry, rank)
        }
        return ranked
    }

    /** The rows shown: the first [limit], or all when [limit] is `null`, 0 or less. */
    fun shown(ranked: List<MonarchLeaderboardRanked>, limit: Int?): List<MonarchLeaderboardRanked> =
        if (limit != null && limit > 0) ranked.take(limit) else ranked

    /** How many entries are not shown: of [total], or of the entries given. Never below 0. */
    fun hidden(total: Int?, entries: Int, shown: Int): Int = ((total ?: entries) - shown).coerceAtLeast(0)

    /** The length of a bar: the size of [value] against the largest size shown, from 0 to 1. */
    fun share(value: Double, shown: List<MonarchLeaderboardRanked>): Float {
        val max = shown.maxOfOrNull { abs(it.entry.value) } ?: 0.0
        return if (max > 0.0) (abs(value) / max).toFloat() else 0f
    }

    /** Whether [rank] sits on the filled marker. */
    fun isTop(highlightTop: Boolean, rank: Int): Boolean = highlightTop && rank <= TOP_RANKS
}
