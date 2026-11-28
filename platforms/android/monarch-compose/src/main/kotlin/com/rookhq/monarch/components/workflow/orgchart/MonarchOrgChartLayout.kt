package com.rookhq.monarch.components.workflow.orgchart

/** How a `MonarchOrgChart` draws the organisation. */
public enum class MonarchOrgChartLayout {
    /** The chart when the component is at least 640dp wide and the font scale is at most 1.3; the list otherwise. */
    Auto,

    /** Cards under their managers, joined by lines, in a view that scrolls both ways and zooms. */
    Chart,

    /** An indented list of cards: the phone form. */
    List,
}
