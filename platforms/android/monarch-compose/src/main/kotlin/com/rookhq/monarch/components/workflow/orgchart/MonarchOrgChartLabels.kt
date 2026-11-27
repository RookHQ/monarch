package com.rookhq.monarch.components.workflow.orgchart

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchOrgChart`. Every
 * `null` keeps the built-in string `monarch_orgchart_<key>`.
 *
 * @property label what TalkBack reads for the chart, "Organisation chart".
 * @property vacant the line on an open position, "Open position".
 * @property dashed what TalkBack adds for a dashed card, "Temporary reporting line".
 * @property alsoReportsTo the dotted-line text, given the secondary managers' names: "Also reports to Noor Ali".
 * @property reports the number of direct reports: "3 direct reports".
 * @property expanded the state of a manager whose team is shown, "Team shown".
 * @property collapsed the state of a manager whose team is hidden, "Team hidden".
 * @property loading the state of a team being loaded, "Loading team".
 * @property expand the name of the button and action that show a team, given the manager's name.
 * @property collapse the name of the button and action that hide a team, given the manager's name.
 * @property move the name of the move action, "Move".
 * @property moveHint the visible hint shown when moving is on.
 * @property loadFailed the announcement when a team fails to load, given the manager's name.
 * @property zoomIn the name of the zoom-in button, "Zoom in".
 * @property zoomOut the name of the zoom-out button, "Zoom out".
 * @property zoomReset the name of the reset button, given the zoom level text: "Reset zoom, 80%".
 * @property zoomInSymbol the visible text of the zoom-in button without an icon, "+".
 * @property zoomOutSymbol the visible text of the zoom-out button without an icon, "−".
 */
@Immutable
public class MonarchOrgChartLabels(
    public val label: String? = null,
    public val vacant: String? = null,
    public val dashed: String? = null,
    public val alsoReportsTo: ((String) -> String)? = null,
    public val reports: ((Int) -> String)? = null,
    public val expanded: String? = null,
    public val collapsed: String? = null,
    public val loading: String? = null,
    public val expand: ((String) -> String)? = null,
    public val collapse: ((String) -> String)? = null,
    public val move: String? = null,
    public val moveHint: String? = null,
    public val loadFailed: ((String) -> String)? = null,
    public val zoomIn: String? = null,
    public val zoomOut: String? = null,
    public val zoomReset: ((String) -> String)? = null,
    public val zoomInSymbol: String? = null,
    public val zoomOutSymbol: String? = null,
)
