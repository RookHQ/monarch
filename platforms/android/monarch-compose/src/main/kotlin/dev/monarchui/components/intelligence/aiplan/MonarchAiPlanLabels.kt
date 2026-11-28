package dev.monarchui.components.intelligence.aiplan

import androidx.compose.runtime.Immutable
import dev.monarchui.components.interaction.reorderablelist.MonarchReorderableListLabels

/**
 * Replacements for the built-in strings of a `MonarchAiPlan`. Every `null`
 * keeps the built-in string `monarch_aiplan_<key>`.
 *
 * @property title the title and the spoken name of the plan, "Proposed plan".
 * @property description the line under the title while pending.
 * @property editHint added to the description while the plan can be edited.
 * @property approvedNotice the line under the title once approved; also announced.
 * @property rejectedNotice the line under the title once rejected; also announced.
 * @property stepLabel the spoken name of a step's text, given its position: "Step 2".
 * @property stepPlaceholder shown in an empty step, "Describe this step".
 * @property untitledStep the name of an empty step in spoken names, given its position: "step 2".
 * @property remove the spoken name of a step's remove button, given the step: "Remove Draft the reply".
 * @property removeText the visible text of the remove button when there is no remove icon, "Remove".
 * @property removed announced after a step is removed, given the step: "Removed Draft the reply".
 * @property addStep the button that adds a step, "Add step".
 * @property approve the button that approves the plan, "Approve plan".
 * @property reject the button that rejects the plan, "Reject".
 * @property emptyPlan the note shown while every step is empty.
 * @property reorder the strings of the reorderable list: the handle's name and the move announcements.
 */
@Immutable
public class MonarchAiPlanLabels(
    public val title: String? = null,
    public val description: String? = null,
    public val editHint: String? = null,
    public val approvedNotice: String? = null,
    public val rejectedNotice: String? = null,
    public val stepLabel: ((position: Int) -> String)? = null,
    public val stepPlaceholder: String? = null,
    public val untitledStep: ((position: Int) -> String)? = null,
    public val remove: ((step: String) -> String)? = null,
    public val removeText: String? = null,
    public val removed: ((step: String) -> String)? = null,
    public val addStep: String? = null,
    public val approve: String? = null,
    public val reject: String? = null,
    public val emptyPlan: String? = null,
    public val reorder: MonarchReorderableListLabels = MonarchReorderableListLabels(),
)
