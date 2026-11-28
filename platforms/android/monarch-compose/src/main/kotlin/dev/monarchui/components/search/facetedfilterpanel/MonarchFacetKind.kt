package dev.monarchui.components.search.facetedfilterpanel

import androidx.compose.runtime.Immutable

/** What a facet of a `MonarchFacetedFilterPanel` filters by: checked options, or a range between two bounds. */
public sealed interface MonarchFacetKind {
    /**
     * A list of checkboxes. The selection is the set of checked option ids.
     *
     * @property options the choices, in order.
     * @property maxVisible how many choices show before "Show more". Checked choices always show.
     */
    @Immutable
    public class Options(
        public val options: List<MonarchFacetOption>,
        public val maxVisible: Int = DEFAULT_MAX_VISIBLE,
    ) : MonarchFacetKind

    /**
     * A minimum and a maximum on a Material 3 `RangeSlider`. The selection is the chosen range.
     *
     * @property bounds the lowest and highest values that can be chosen.
     * @property step the distance between values that can be chosen; 0 or less is continuous.
     * @property unit shown after the values and read with them, such as "MB" or "days". Already localised.
     */
    @Immutable
    public class Range(
        public val bounds: ClosedFloatingPointRange<Float>,
        public val step: Float = 1f,
        public val unit: String? = null,
    ) : MonarchFacetKind

    /** Defaults of the facet kinds. */
    public companion object {
        /** The options shown before "Show more" when nothing else is given: 6, as on the web. */
        public const val DEFAULT_MAX_VISIBLE: Int = 6
    }
}
