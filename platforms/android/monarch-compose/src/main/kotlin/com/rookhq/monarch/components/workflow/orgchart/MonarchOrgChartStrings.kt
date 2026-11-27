package com.rookhq.monarch.components.workflow.orgchart

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import com.rookhq.monarch.R
import com.rookhq.monarch.internal.monarchString

/** The strings of a `MonarchOrgChart`, built in or replaced, resolved once per composition. */
@Suppress("LongParameterList") // One value per built-in string.
@Immutable
internal class MonarchOrgChartStrings(
    val label: String,
    val vacant: String,
    val dashed: String,
    val expanded: String,
    val collapsed: String,
    val loading: String,
    val move: String,
    val moveHint: String,
    val zoomIn: String,
    val zoomOut: String,
    val zoomInSymbol: String,
    val zoomOutSymbol: String,
    val alsoReportsTo: (String) -> String,
    val reports: (Int) -> String,
    val expand: (String) -> String,
    val collapse: (String) -> String,
    val loadFailed: (String) -> String,
    val zoomReset: (String) -> String,
)

/** Resolves [labels] against the built-in strings. */
@Composable
internal fun monarchOrgChartStrings(labels: MonarchOrgChartLabels): MonarchOrgChartStrings {
    val alsoReportsTo = stringResource(R.string.monarch_orgchart_also_reports_to)
    val expand = stringResource(R.string.monarch_orgchart_expand)
    val collapse = stringResource(R.string.monarch_orgchart_collapse)
    val loadFailed = stringResource(R.string.monarch_orgchart_load_failed)
    val zoomReset = stringResource(R.string.monarch_orgchart_zoom_reset)
    val resources = LocalResources.current
    return MonarchOrgChartStrings(
        label = monarchString(labels.label, R.string.monarch_orgchart_label),
        vacant = monarchString(labels.vacant, R.string.monarch_orgchart_vacant),
        dashed = monarchString(labels.dashed, R.string.monarch_orgchart_dashed),
        expanded = monarchString(labels.expanded, R.string.monarch_orgchart_expanded),
        collapsed = monarchString(labels.collapsed, R.string.monarch_orgchart_collapsed),
        loading = monarchString(labels.loading, R.string.monarch_orgchart_loading),
        move = monarchString(labels.move, R.string.monarch_orgchart_move),
        moveHint = monarchString(labels.moveHint, R.string.monarch_orgchart_move_hint),
        zoomIn = monarchString(labels.zoomIn, R.string.monarch_orgchart_zoom_in),
        zoomOut = monarchString(labels.zoomOut, R.string.monarch_orgchart_zoom_out),
        zoomInSymbol = monarchString(labels.zoomInSymbol, R.string.monarch_orgchart_zoom_in_symbol),
        zoomOutSymbol = monarchString(labels.zoomOutSymbol, R.string.monarch_orgchart_zoom_out_symbol),
        alsoReportsTo = labels.alsoReportsTo ?: { alsoReportsTo.format(it) },
        reports = labels.reports ?: { resources.getQuantityString(R.plurals.monarch_orgchart_reports, it, it) },
        expand = labels.expand ?: { expand.format(it) },
        collapse = labels.collapse ?: { collapse.format(it) },
        loadFailed = labels.loadFailed ?: { loadFailed.format(it) },
        zoomReset = labels.zoomReset ?: { zoomReset.format(it) },
    )
}
