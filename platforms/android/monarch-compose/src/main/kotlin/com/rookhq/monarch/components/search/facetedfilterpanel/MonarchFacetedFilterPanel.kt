package com.rookhq.monarch.components.search.facetedfilterpanel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.layout.divider.MonarchDivider
import com.rookhq.monarch.components.layout.divider.MonarchDividerVariant
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme

/**
 * A panel of filter groups: checkboxes with match counts and minimum to
 * maximum range sliders, each group collapsible and clearable, with a Clear
 * all button.
 *
 * Built by Monarch from Material 3 parts: each option is a Material 3
 * `Checkbox` in a row that toggles as one element, each range a Material 3
 * `RangeSlider`, and the buttons are `MonarchButton`s. Group headers work as
 * in `MonarchAccordion` (a heading and a button with the state Expanded or
 * Collapsed) and also show the number of active filters. On a phone, show
 * the panel in a `ModalBottomSheet` opened from a Filters button, inside a
 * `verticalScroll`; on a wide screen it can sit beside the results. The
 * panel has no scrolling of its own.
 *
 * @param facets the groups, in order.
 * @param value the active filters by facet id. A facet without a filter has no entry.
 * @param onValueChange called with the whole new value after every change.
 * @param modifier the modifier of the panel.
 * @param enabled whether the filters can be changed and groups opened.
 * @param showTitle shows the Filters heading. Hide it inside a sheet whose title already says it; Clear all stays,
 * and TalkBack still reads the panel's name.
 * @param expandIcon an icon at the start of each group header: a chevron pointing right, which turns a quarter
 * turn to point down while the group is open, as on the web. The state is also said in words.
 * @param labels replacements for the built-in strings.
 * @param footer content after the groups, such as an Apply button in a sheet.
 */
@Suppress("LongParameterList", "LongMethod")
@Composable
public fun MonarchFacetedFilterPanel(
    facets: List<MonarchFacet>,
    value: Map<String, MonarchFacetSelection>,
    onValueChange: (Map<String, MonarchFacetSelection>) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showTitle: Boolean = true,
    expandIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.ChevronRight,
    labels: MonarchFacetedFilterPanelLabels = MonarchFacetedFilterPanelLabels(),
    footer: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val tokens = MonarchFacetedFilterPanelTokens
    val colors = tokens.colors(MonarchTheme.colors, enabled)
    val locale = LocalConfiguration.current.locales[0]
    val text = facetedFilterPanelText(labels)
    var open by rememberSaveable { mutableStateOf(facets.filter { it.initiallyExpanded }.map { it.id }.toSet()) }
    var showAll by rememberSaveable { mutableStateOf(emptySet<String>()) }
    val total = FacetedFilterPanelModel.totalActive(facets, value)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                isTraversalGroup = true
                if (!showTitle) contentDescription = text.title
            },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showTitle) {
                Text(
                    text = text.title,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() },
                    style = MonarchTheme.typography.heading,
                    color = colors.title,
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            MonarchButton(
                text = text.clearAll,
                onClick = { onValueChange(emptyMap()) },
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
                enabled = enabled && total > 0,
            )
        }
        facets.forEachIndexed { index, facet ->
            if (index > 0) MonarchDivider(variant = MonarchDividerVariant.Subtle)
            FacetedFilterPanelGroup(
                facet = facet,
                value = value,
                onValueChange = onValueChange,
                state = FacetedFilterPanelGroupState(
                    open = facet.id in open,
                    onOpenChange = { open = if (it) open + facet.id else open - facet.id },
                    showAll = facet.id in showAll,
                    onShowAllChange = { showAll = if (it) showAll + facet.id else showAll - facet.id },
                ),
                enabled = enabled,
                expandIcon = expandIcon,
                text = text,
                colors = colors,
                locale = locale,
            )
        }
        if (footer != null) footer()
    }
}

/** The resolved built-in strings. */
@Suppress("LongParameterList")
internal class FacetedFilterPanelText(
    val title: String,
    val clearAll: String,
    val clearGroup: String,
    val clearGroupLabel: (String) -> String,
    val activeCount: (Int) -> String,
    val noOptions: String,
    val range: (String, String) -> String,
    val minimum: String,
    val maximum: String,
    val toggles: FacetedFilterPanelToggleText,
)

/** The resolved texts of things that open and close: groups and Show more. */
internal class FacetedFilterPanelToggleText(
    val expanded: String,
    val collapsed: String,
    val showMore: (Int) -> String,
    val showFewer: String,
)

@Composable
private fun facetedFilterPanelText(labels: MonarchFacetedFilterPanelLabels): FacetedFilterPanelText {
    val clearGroupLabel = stringResource(R.string.monarch_facetedfilterpanel_clear_group_label)
    val range = stringResource(R.string.monarch_facetedfilterpanel_range)
    val resources = LocalResources.current
    return FacetedFilterPanelText(
        title = monarchString(labels.title, R.string.monarch_facetedfilterpanel_title),
        clearAll = monarchString(labels.clearAll, R.string.monarch_facetedfilterpanel_clear_all),
        clearGroup = monarchString(labels.clearGroup, R.string.monarch_facetedfilterpanel_clear_group),
        clearGroupLabel = labels.clearGroupLabel ?: { clearGroupLabel.format(it) },
        activeCount = labels.activeCount ?: { count ->
            resources.getQuantityString(R.plurals.monarch_facetedfilterpanel_active_count, count, count)
        },
        noOptions = monarchString(labels.noOptions, R.string.monarch_facetedfilterpanel_no_options),
        range = labels.range ?: { start, end -> range.format(start, end) },
        minimum = monarchString(labels.minimum, R.string.monarch_facetedfilterpanel_minimum),
        maximum = monarchString(labels.maximum, R.string.monarch_facetedfilterpanel_maximum),
        toggles = FacetedFilterPanelToggleText(
            expanded = monarchString(labels.expanded, R.string.monarch_facetedfilterpanel_expanded),
            collapsed = monarchString(labels.collapsed, R.string.monarch_facetedfilterpanel_collapsed),
            showMore = labels.showMore ?: { count ->
                resources.getQuantityString(R.plurals.monarch_facetedfilterpanel_show_more, count, count)
            },
            showFewer = monarchString(labels.showFewer, R.string.monarch_facetedfilterpanel_show_fewer),
        ),
    )
}
