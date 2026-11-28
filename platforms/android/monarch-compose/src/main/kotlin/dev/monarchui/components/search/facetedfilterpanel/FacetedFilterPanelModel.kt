package dev.monarchui.components.search.facetedfilterpanel

/**
 * The selection rules of MonarchFacetedFilterPanel. Pure, so they are
 * tested without a view. A facet without a filter has no entry: no checked
 * options, or a range equal to the bounds.
 */
internal object FacetedFilterPanelModel {
    /** The number of active filters of [facet]: its checked options, or 1 for a narrowed range. */
    fun activeCount(facet: MonarchFacet, value: Map<String, MonarchFacetSelection>): Int =
        when (val selection = value[facet.id]) {
            is MonarchFacetSelection.Options -> selection.ids.size
            is MonarchFacetSelection.Range -> if (isFullRange(facet, selection.range)) 0 else 1
            null -> 0
        }

    /** The active filters of every facet. */
    fun totalActive(facets: List<MonarchFacet>, value: Map<String, MonarchFacetSelection>): Int =
        facets.sumOf { activeCount(it, value) }

    /** The checked option ids of [facet]. */
    fun checked(facet: MonarchFacet, value: Map<String, MonarchFacetSelection>): Set<String> =
        (value[facet.id] as? MonarchFacetSelection.Options)?.ids.orEmpty()

    /** [value] with option [id] of [facet] checked or not; an empty set removes the entry. */
    fun toggle(
        facet: MonarchFacet,
        id: String,
        checked: Boolean,
        value: Map<String, MonarchFacetSelection>,
    ): Map<String, MonarchFacetSelection> {
        val ids = checked(facet, value).let { if (checked) it + id else it - id }
        return set(facet.id, ids.takeIf { it.isNotEmpty() }?.let { MonarchFacetSelection.Options(it) }, value)
    }

    /** The range of [facet]: its selection, or its bounds. */
    fun range(facet: MonarchFacet, kind: MonarchFacetKind.Range, value: Map<String, MonarchFacetSelection>) =
        (value[facet.id] as? MonarchFacetSelection.Range)?.range ?: kind.bounds

    /** [value] with [range] for [facet]; the full bounds remove the entry. */
    fun setRange(
        facet: MonarchFacet,
        range: ClosedFloatingPointRange<Float>,
        value: Map<String, MonarchFacetSelection>,
    ): Map<String, MonarchFacetSelection> =
        set(facet.id, if (isFullRange(facet, range)) null else MonarchFacetSelection.Range(range), value)

    /** [value] without the entry of [facet]. */
    fun clear(facet: MonarchFacet, value: Map<String, MonarchFacetSelection>): Map<String, MonarchFacetSelection> =
        set(facet.id, null, value)

    private fun set(
        id: String,
        selection: MonarchFacetSelection?,
        value: Map<String, MonarchFacetSelection>,
    ): Map<String, MonarchFacetSelection> = if (selection == null) value - id else value + (id to selection)

    private fun isFullRange(facet: MonarchFacet, range: ClosedFloatingPointRange<Float>): Boolean {
        val kind = facet.kind as? MonarchFacetKind.Range ?: return false
        return range.start <= kind.bounds.start && range.endInclusive >= kind.bounds.endInclusive
    }

    /**
     * The options shown: the first [MonarchFacetKind.Options.maxVisible],
     * plus every checked option, or all while [showAll].
     */
    fun visible(kind: MonarchFacetKind.Options, checked: Set<String>, showAll: Boolean): List<MonarchFacetOption> =
        if (showAll) {
            kind.options
        } else {
            kind.options.filterIndexed { index, option -> index < kind.maxVisible || option.id in checked }
        }
}
