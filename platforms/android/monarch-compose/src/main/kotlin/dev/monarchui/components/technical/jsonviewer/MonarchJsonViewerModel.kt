package dev.monarchui.components.technical.jsonviewer

/**
 * One visible row of the flattened tree.
 *
 * @property more whether this is the "Show more" row after the shown children of a container.
 * @property id the path id of the node; for a "Show more" row, the id of its container followed by `#more`.
 * @property segments the path of the node, or of the container of a "Show more" row.
 * @property parentId the id of the container.
 * @property name the key or index of the node; `null` for the root and for "Show more".
 * @property value the node's value.
 * @property depth the nesting level: 0 for the root.
 * @property count the number of children of a container.
 * @property shown for "Show more": how many children are shown before it.
 * @property remaining for "Show more": how many children are still hidden.
 */
internal data class MonarchJsonViewerRow(
    val more: Boolean,
    val id: String,
    val segments: List<MonarchJsonViewerSegment>,
    val parentId: String?,
    val name: MonarchJsonViewerSegment?,
    val value: MonarchJsonValue,
    val depth: Int,
    val count: Int,
    val shown: Int = 0,
    val remaining: Int = 0,
) {
    /** Whether the row opens and closes. */
    val expandable: Boolean get() = !more && count > 0
}

/**
 * What a search found.
 *
 * @property matched the ids of the matching nodes.
 * @property open the ids of containers opened to show a match.
 * @property reveal for each container, how many children must be shown to reach its last match.
 * @property count the number of matches.
 * @property capped whether the search stopped at the most matches.
 */
internal data class MonarchJsonViewerSearch(
    val matched: Set<String> = emptySet(),
    val open: Set<String> = emptySet(),
    val reveal: Map<String, Int> = emptyMap(),
    val count: Int = 0,
    val capped: Boolean = false,
)

/** The open state of the tree: what the user opened or closed, how many children are shown, and the search. */
internal data class MonarchJsonViewerOpenState(
    val overrides: Map<String, Boolean>,
    val limits: Map<String, Int>,
    val search: MonarchJsonViewerSearch,
    val baseDepth: Int,
    val maxChildren: Int,
)

/**
 * The pure tree logic of MonarchJsonViewer: the search, the flattened rows and the matches in a text. Only open
 * containers are walked, so the cost of the rows follows what is visible, not the size of the document.
 */
internal object MonarchJsonViewerModel {
    /** The suffix of the id of a "Show more" row. */
    const val MoreSuffix: String = "#more"

    /**
     * Walks the whole document once and finds the nodes whose key or primitive value contains [needle] (already lower
     * case and trimmed), stopping after [maxMatches].
     */
    fun search(value: MonarchJsonValue, needle: String, maxMatches: Int): MonarchJsonViewerSearch {
        if (needle.isEmpty()) return MonarchJsonViewerSearch()
        val matched = mutableSetOf<String>()
        val open = mutableSetOf<String>()
        val reveal = mutableMapOf<String, Int>()
        fun capped() = matched.size >= maxMatches
        fun walk(node: MonarchJsonValue, segments: List<MonarchJsonViewerSegment>): Boolean {
            if (capped()) return false
            val id = MonarchJsonViewerPaths.id(segments)
            val name = segments.lastOrNull()?.let(MonarchJsonViewerPaths::name)
            val key = name?.lowercase()?.contains(needle) == true
            val hit = key || MonarchJsonViewerPaths.searchableText(node)?.lowercase()?.contains(needle) == true
            if (hit) matched += id
            var childHit = false
            for ((offset, child) in MonarchJsonViewerPaths.children(node).withIndex()) {
                if (walk(child.second, segments + child.first)) {
                    childHit = true
                    reveal[id] = maxOf(reveal[id] ?: 0, offset + 1)
                }
                if (capped()) break
            }
            if (childHit) open += id
            return hit || childHit
        }
        walk(value, emptyList())
        return MonarchJsonViewerSearch(matched, open, reveal, matched.size, capped())
    }

    /** Whether a container is open: the user's choice first, then a search that reaches inside it, then the depth. */
    fun open(id: String, depth: Int, state: MonarchJsonViewerOpenState): Boolean =
        state.overrides[id] ?: (id in state.search.open || depth < state.baseDepth)

    /**
     * The visible rows: the root, and the children of every open container up to its limit, followed by a "Show
     * more" row when some are hidden.
     */
    fun rows(value: MonarchJsonValue, state: MonarchJsonViewerOpenState): List<MonarchJsonViewerRow> {
        val out = mutableListOf<MonarchJsonViewerRow>()
        fun visit(node: MonarchJsonValue, segments: List<MonarchJsonViewerSegment>, parentId: String?, depth: Int) {
            val id = MonarchJsonViewerPaths.id(segments)
            val count = MonarchJsonViewerPaths.childCount(node)
            out += MonarchJsonViewerRow(false, id, segments, parentId, segments.lastOrNull(), node, depth, count)
            if (count == 0 || !open(id, depth, state)) return
            val limit = maxOf(state.limits[id] ?: state.maxChildren, state.search.reveal[id] ?: 0, 1)
            val children = MonarchJsonViewerPaths.children(node)
            val shown = minOf(children.size, limit)
            children.take(shown).forEach { (segment, child) -> visit(child, segments + segment, id, depth + 1) }
            if (shown < children.size) {
                out += MonarchJsonViewerRow(
                    more = true,
                    id = id + MoreSuffix,
                    segments = segments,
                    parentId = id,
                    name = null,
                    value = MonarchJsonValue.Null,
                    depth = depth + 1,
                    count = 0,
                    shown = shown,
                    remaining = children.size - shown,
                )
            }
        }
        visit(value, emptyList(), null, 0)
        return out
    }

    /** The ranges of [text] that contain [needle], ignoring case. */
    fun matchRanges(text: String, needle: String): List<IntRange> {
        if (needle.isEmpty()) return emptyList()
        val ranges = mutableListOf<IntRange>()
        var start = text.indexOf(needle, ignoreCase = true)
        while (start >= 0) {
            ranges += start until start + needle.length
            start = text.indexOf(needle, start + needle.length, ignoreCase = true)
        }
        return ranges
    }
}
