package dev.monarchui.components.files.diffviewer

/** Above this many line pairs the line diff falls back to remove all, add all (as on the web). */
internal const val DiffViewerMaxLinePairs: Long = 1_000_000L

/** Above this many word pairs a changed line is not split into words (as on the web). */
internal const val DiffViewerMaxWordPairs: Long = 250_000L

/** One step of an edit script: keep, remove or add [value]. */
internal class DiffViewerOp<T>(val kind: MonarchDiffViewerLineKind, val value: T)

/**
 * The edit script from [a] to [b] along a longest common subsequence, ported from the web's `diffSequences`.
 * When a removal and an addition are both optimal, the removal comes first. More than [maxPairs] item pairs
 * skip the table and remove all of [a], then add all of [b].
 */
internal fun <T> diffViewerSequences(a: List<T>, b: List<T>, maxPairs: Long): List<DiffViewerOp<T>> {
    val n = a.size
    val m = b.size
    val out = ArrayList<DiffViewerOp<T>>(n + m)
    if (n.toLong() * m.toLong() > maxPairs) {
        a.forEach { out += DiffViewerOp(MonarchDiffViewerLineKind.Removed, it) }
        b.forEach { out += DiffViewerOp(MonarchDiffViewerLineKind.Added, it) }
        return out
    }
    // lcs[i * w + j] is the length of the longest common subsequence of a[i..] and b[j..].
    val w = m + 1
    val lcs = IntArray((n + 1) * w)
    for (i in n - 1 downTo 0) {
        for (j in m - 1 downTo 0) {
            lcs[i * w + j] = if (a[i] == b[j]) {
                lcs[(i + 1) * w + j + 1] + 1
            } else {
                maxOf(lcs[(i + 1) * w + j], lcs[i * w + j + 1])
            }
        }
    }
    var i = 0
    var j = 0
    while (i < n && j < m) {
        when {
            a[i] == b[j] -> {
                out += DiffViewerOp(MonarchDiffViewerLineKind.Same, a[i])
                i++
                j++
            }
            lcs[(i + 1) * w + j] >= lcs[i * w + j + 1] -> out += DiffViewerOp(MonarchDiffViewerLineKind.Removed, a[i++])
            else -> out += DiffViewerOp(MonarchDiffViewerLineKind.Added, b[j++])
        }
    }
    while (i < n) out += DiffViewerOp(MonarchDiffViewerLineKind.Removed, a[i++])
    while (j < m) out += DiffViewerOp(MonarchDiffViewerLineKind.Added, b[j++])
    return out
}

/** A piece of a changed line in a word diff. */
internal data class DiffViewerSegment(val kind: MonarchDiffViewerLineKind, val text: String)

/** Words, runs of white space and single other characters: the units of the word diff. */
private val DiffViewerToken = Regex("""\s+|[\p{L}\p{N}_]+|[^\s\p{L}\p{N}_]""")

/** The word diff of two lines; neighbouring pieces of the same kind are joined. */
internal fun diffViewerWords(before: String, after: String): List<DiffViewerSegment> {
    val a = DiffViewerToken.findAll(before).map { it.value }.toList()
    val b = DiffViewerToken.findAll(after).map { it.value }.toList()
    val out = ArrayList<DiffViewerSegment>()
    diffViewerSequences(a, b, DiffViewerMaxWordPairs).forEach { op ->
        val last = out.lastOrNull()
        if (last != null && last.kind == op.kind) {
            out[out.lastIndex] = DiffViewerSegment(op.kind, last.text + op.value)
        } else {
            out += DiffViewerSegment(op.kind, op.value)
        }
    }
    return out
}

/** The number of added and removed lines. */
internal data class DiffViewerStats(val added: Int, val removed: Int) {
    val changed: Boolean get() = added > 0 || removed > 0
}

/** Counts the added and removed lines. */
internal fun diffViewerStats(lines: List<MonarchDiffViewerLine>): DiffViewerStats = DiffViewerStats(
    added = lines.count { it.kind == MonarchDiffViewerLineKind.Added },
    removed = lines.count { it.kind == MonarchDiffViewerLineKind.Removed },
)

/** The changes of [lines]: each run of non-same lines, split into its removed and added line indexes. */
internal class DiffViewerRun(val removed: List<Int>, val added: List<Int>)

internal fun diffViewerRuns(lines: List<MonarchDiffViewerLine>, from: Int): Pair<DiffViewerRun, Int> {
    val removed = ArrayList<Int>()
    val added = ArrayList<Int>()
    var i = from
    while (i < lines.size && lines[i].kind != MonarchDiffViewerLineKind.Same) {
        if (lines[i].kind == MonarchDiffViewerLineKind.Removed) removed += i else added += i
        i++
    }
    return DiffViewerRun(removed, added) to i
}

/**
 * The word segments of each changed line that pairs up: the k-th removed line of a change with its k-th added
 * line. Keyed by the line's index in [lines]. Empty for [MonarchDiffViewerGranularity.Line].
 */
internal fun diffViewerWordMap(
    lines: List<MonarchDiffViewerLine>,
    granularity: MonarchDiffViewerGranularity,
): Map<Int, List<DiffViewerSegment>> {
    if (granularity != MonarchDiffViewerGranularity.Word) return emptyMap()
    val map = HashMap<Int, List<DiffViewerSegment>>()
    var i = 0
    while (i < lines.size) {
        if (lines[i].kind == MonarchDiffViewerLineKind.Same) {
            i++
            continue
        }
        val (run, next) = diffViewerRuns(lines, i)
        for (k in 0 until minOf(run.removed.size, run.added.size)) {
            val segments = diffViewerWords(lines[run.removed[k]].text, lines[run.added[k]].text)
            map[run.removed[k]] = segments
            map[run.added[k]] = segments
        }
        i = next
    }
    return map
}

/** One row of the viewer. Line indexes point into the diff's lines. */
internal sealed interface DiffViewerRow {
    /** The change this row starts, from 0, or `null`. */
    val hunk: Int?

    /** A unified row: one line. */
    data class Line(val line: Int, override val hunk: Int?) : DiffViewerRow

    /** A side by side row: the earlier line beside the later one; `null` is an empty side. */
    data class Pair(val old: Int?, val new: Int?, override val hunk: Int?) : DiffViewerRow

    /** A folded run of [count] unchanged lines; [id] is the run's number for expanding it. */
    data class Gap(val id: Int, val count: Int) : DiffViewerRow {
        override val hunk: Int? get() = null
    }
}

/** The rows and the number of changes. */
internal class DiffViewerRows(val rows: List<DiffViewerRow>, val hunks: Int)

/**
 * Groups consecutive changes into hunks and folds unchanged runs longer than [context] around them, ported
 * from the web. A run is folded only when it is more than one line longer than what stays shown, and not when
 * its [id][DiffViewerRow.Gap.id] is in [expanded]. A `null` [context] never folds.
 */
internal fun diffViewerRows(
    lines: List<MonarchDiffViewerLine>,
    split: Boolean,
    context: Int?,
    expanded: Set<Int>,
): DiffViewerRows {
    val builder = DiffViewerRowBuilder(split, context?.coerceAtLeast(0), expanded)
    var i = 0
    while (i < lines.size) {
        if (lines[i].kind == MonarchDiffViewerLineKind.Same) {
            val start = i
            while (i < lines.size && lines[i].kind == MonarchDiffViewerLineKind.Same) i++
            builder.same(start, i, atStart = start == 0, atEnd = i == lines.size)
        } else {
            val (run, next) = diffViewerRuns(lines, i)
            builder.change(run)
            i = next
        }
    }
    return DiffViewerRows(builder.rows, builder.hunks)
}

/** Collects the rows of [diffViewerRows]. */
private class DiffViewerRowBuilder(val split: Boolean, val keep: Int?, val expanded: Set<Int>) {
    val rows = ArrayList<DiffViewerRow>()
    var hunks = 0
    private var gapId = 0

    private fun addSame(index: Int) {
        rows += if (split) DiffViewerRow.Pair(index, index, null) else DiffViewerRow.Line(index, null)
    }

    /** The unchanged lines from [start] until [end], folded when the run is long enough. */
    fun same(start: Int, end: Int, atStart: Boolean, atEnd: Boolean) {
        val id = gapId++
        val lead = if (atStart) 0 else keep ?: 0
        val trail = if (atEnd) 0 else keep ?: 0
        val length = end - start
        if (keep == null || length <= lead + trail + 1 || id in expanded) {
            for (index in start until end) addSame(index)
            return
        }
        for (index in start until start + lead) addSame(index)
        rows += DiffViewerRow.Gap(id, length - lead - trail)
        for (index in end - trail until end) addSame(index)
    }

    /** One change: pairs side by side, or its removed lines then its added lines. */
    fun change(run: DiffViewerRun) {
        val hunk = hunks++
        if (split) {
            for (k in 0 until maxOf(run.removed.size, run.added.size)) {
                rows += DiffViewerRow.Pair(run.removed.getOrNull(k), run.added.getOrNull(k), if (k == 0) hunk else null)
            }
        } else {
            (run.removed + run.added).forEachIndexed { k, index ->
                rows += DiffViewerRow.Line(index, if (k == 0) hunk else null)
            }
        }
    }
}
