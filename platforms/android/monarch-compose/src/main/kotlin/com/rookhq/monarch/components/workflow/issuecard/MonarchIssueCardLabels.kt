package com.rookhq.monarch.components.workflow.issuecard

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.workflow.prioritypicker.MonarchPriorityLevel

/**
 * Replacements for the built-in strings of a `MonarchIssueCard`. Every
 * `null` keeps the built-in string `monarch_issuecard_<key>` (or, for the
 * priority names, the `MonarchPriorityPicker` ones).
 *
 * @property due the visible due text, given the formatted day: "Due Mar 12".
 * @property overdue the word added when the due day has passed, "Overdue".
 * @property tags what TalkBack reads for the tags, given their names joined with commas: "Labels: Bug, Auth".
 * @property unassigned what TalkBack reads for an empty assignee slot, "Unassigned".
 * @property assignee what TalkBack reads for the assignee, given the name: "Assigned to Ana Ruiz".
 * @property status what TalkBack reads for a read-only status, given its name: "Status: In progress". An editable
 * status uses the `MonarchStatusPicker` trigger text.
 * @property priority what TalkBack reads for the priority, given the level's name: "Priority: High".
 * @property priorityLevel the name of each priority level; `null` keeps the `MonarchPriorityPicker` names.
 */
@Immutable
public class MonarchIssueCardLabels(
    public val due: ((String) -> String)? = null,
    public val overdue: String? = null,
    public val tags: ((String) -> String)? = null,
    public val unassigned: String? = null,
    public val assignee: ((String) -> String)? = null,
    public val status: ((String) -> String)? = null,
    public val priority: ((String) -> String)? = null,
    public val priorityLevel: ((MonarchPriorityLevel) -> String)? = null,
)
