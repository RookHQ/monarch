package com.rookhq.monarch.components.workflow.gantttimeline

/**
 * The vertical layout of the chart in pixels: the header, the milestone lane and each row, measured from the row
 * labels so a label that wraps makes its whole row taller.
 */
internal class GanttTimelineGeometry(
    val header: Int,
    val laneHeight: Int,
    val rowTops: IntArray,
    val rowHeights: IntArray,
    val height: Int,
)
