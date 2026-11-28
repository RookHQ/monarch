package dev.monarchui.components.workflow.issuecard

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter

/** Who an issue in a `MonarchIssueCard` is assigned to. */
@Immutable
public sealed interface MonarchIssueCardAssignee {
    /**
     * A person, shown as an avatar.
     *
     * @property name the person's name, already localised. Read as "Assigned to <name>"; its initials show while
     * there is no [image].
     * @property image the person's photo, loaded by the app; `null` shows the initials.
     */
    @Immutable
    public class Person(public val name: String, public val image: Painter? = null) : MonarchIssueCardAssignee

    /** Nobody: an empty dashed slot, read as "Unassigned". */
    public data object Unassigned : MonarchIssueCardAssignee
}
