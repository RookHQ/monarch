package com.rookhq.monarch.components.search.facetedfilterpanel

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/**
 * The colours of a faceted filter panel.
 *
 * @property title the panel heading and group names.
 * @property active the "2 selected" count of a group with filters.
 * @property label an option's label.
 * @property count an option's count, the range text and "No options".
 */
@Immutable
internal data class FacetedFilterPanelColors(
    val title: Color,
    val active: Color,
    val label: Color,
    val count: Color,
)

/** Component tokens of MonarchFacetedFilterPanel, built from primitives and roles only. */
internal object MonarchFacetedFilterPanelTokens {
    val Gap: Dp = MonarchTokens.Space.space2.dp
    val RowGap: Dp = MonarchTokens.Space.space3.dp
    val HeaderVerticalPadding: Dp = MonarchTokens.Space.space2.dp
    val GroupBottomPadding: Dp = MonarchTokens.Space.space2.dp

    /** A quarter turn of the expand icon (a chevron pointing right) while the group is open. */
    const val OPEN_DEGREES: Float = 90f

    /**
     * The colours: default text for headings and labels, the strong accent
     * for the active count (also said in words), muted text for counts, and
     * disabled text everywhere while the panel is disabled.
     */
    fun colors(colors: MonarchColors, enabled: Boolean): FacetedFilterPanelColors = FacetedFilterPanelColors(
        title = if (enabled) colors.textDefault else colors.textDisabled,
        active = if (enabled) colors.accentStrong else colors.textDisabled,
        label = if (enabled) colors.textDefault else colors.textDisabled,
        count = if (enabled) colors.textMuted else colors.textDisabled,
    )

    /** The colour of one option's label and count: disabled when the option or the panel is. */
    fun optionColor(colors: MonarchColors, enabled: Boolean, muted: Boolean): Color = when {
        !enabled -> colors.textDisabled
        muted -> colors.textMuted
        else -> colors.textDefault
    }
}
