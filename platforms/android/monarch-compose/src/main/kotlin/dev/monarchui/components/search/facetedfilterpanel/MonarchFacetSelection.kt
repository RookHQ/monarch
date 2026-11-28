package dev.monarchui.components.search.facetedfilterpanel

import androidx.compose.runtime.Immutable

/** The active filter of one facet of a `MonarchFacetedFilterPanel`. A facet without a filter has no entry. */
public sealed interface MonarchFacetSelection {
    /**
     * The checked options of an options facet; never empty in a value the panel reports.
     *
     * @property ids the ids of the checked options.
     */
    @Immutable
    public class Options(public val ids: Set<String>) : MonarchFacetSelection {
        override fun equals(other: Any?): Boolean = other is Options && other.ids == ids

        override fun hashCode(): Int = ids.hashCode()

        override fun toString(): String = "Options($ids)"
    }

    /**
     * The chosen range of a range facet; never equal to the facet's bounds in a value the panel reports.
     *
     * @property range the minimum and maximum.
     */
    @Immutable
    public class Range(public val range: ClosedFloatingPointRange<Float>) : MonarchFacetSelection {
        override fun equals(other: Any?): Boolean = other is Range && other.range == range

        override fun hashCode(): Int = range.hashCode()

        override fun toString(): String = "Range($range)"
    }
}
