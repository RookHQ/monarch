package com.rookhq.monarch.components.workflow.orgchart

import androidx.compose.runtime.Immutable

/** Where a person was moved in a `MonarchOrgChart`. The app decides and updates the nodes. */
@Immutable
public sealed interface MonarchOrgChartMoveTarget {
    /**
     * Dropped on another person's card: [id] is the new manager.
     *
     * @property id the id of the new manager.
     */
    public data class Manager(public val id: String) : MonarchOrgChartMoveTarget

    /** Dropped on the empty background: the person becomes top level. */
    public data object TopLevel : MonarchOrgChartMoveTarget

    /** Move was chosen from the card's TalkBack actions: the app asks where, for example in a dialog. */
    public data object Unspecified : MonarchOrgChartMoveTarget
}
