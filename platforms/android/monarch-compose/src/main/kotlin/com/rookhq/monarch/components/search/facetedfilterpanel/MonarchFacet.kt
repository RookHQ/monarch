package com.rookhq.monarch.components.search.facetedfilterpanel

import androidx.compose.runtime.Immutable

/**
 * One group of a `MonarchFacetedFilterPanel`, such as Status or File size.
 *
 * @property id the key of the facet in the selection. Unique in the panel.
 * @property label the group heading, already localised.
 * @property kind checked options or a range.
 * @property initiallyExpanded whether the group starts open. Later the person opens and closes it.
 */
@Immutable
public class MonarchFacet(
    public val id: String,
    public val label: String,
    public val kind: MonarchFacetKind,
    public val initiallyExpanded: Boolean = true,
) {
    /** Factories for the two kinds of facet. */
    public companion object {
        /**
         * A facet of checkboxes.
         *
         * @param id the key of the facet in the selection.
         * @param label the group heading.
         * @param options the choices, in order.
         * @param maxVisible how many choices show before "Show more".
         * @param initiallyExpanded whether the group starts open.
         */
        public fun options(
            id: String,
            label: String,
            options: List<MonarchFacetOption>,
            maxVisible: Int = MonarchFacetKind.DEFAULT_MAX_VISIBLE,
            initiallyExpanded: Boolean = true,
        ): MonarchFacet = MonarchFacet(id, label, MonarchFacetKind.Options(options, maxVisible), initiallyExpanded)

        /**
         * A facet with a minimum and a maximum.
         *
         * @param id the key of the facet in the selection.
         * @param label the group heading.
         * @param bounds the lowest and highest values.
         * @param step the distance between values; 0 or less is continuous.
         * @param unit shown after the values, such as "MB".
         * @param initiallyExpanded whether the group starts open.
         */
        @Suppress("LongParameterList")
        public fun range(
            id: String,
            label: String,
            bounds: ClosedFloatingPointRange<Float>,
            step: Float = 1f,
            unit: String? = null,
            initiallyExpanded: Boolean = true,
        ): MonarchFacet = MonarchFacet(id, label, MonarchFacetKind.Range(bounds, step, unit), initiallyExpanded)
    }
}
