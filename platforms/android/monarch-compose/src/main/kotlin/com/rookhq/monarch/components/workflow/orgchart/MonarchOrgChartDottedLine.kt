package com.rookhq.monarch.components.workflow.orgchart

import androidx.compose.runtime.Immutable

/**
 * A secondary (dotted) reporting line in a `MonarchOrgChart`: drawn as a
 * dashed curve in the chart layout, and read as "Also reports to …" in both
 * layouts.
 *
 * @property from the id of the person who also reports.
 * @property to the id of the secondary manager.
 */
@Immutable
public class MonarchOrgChartDottedLine(public val from: String, public val to: String) {
    override fun equals(
        other: Any?,
    ): Boolean = other is MonarchOrgChartDottedLine && other.from == from && other.to == to

    override fun hashCode(): Int = from.hashCode() * HASH + to.hashCode()

    override fun toString(): String = "MonarchOrgChartDottedLine(from=$from, to=$to)"

    private companion object {
        const val HASH = 31
    }
}
