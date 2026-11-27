package com.rookhq.monarch.components.workflow.approvalchaineditor

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.interaction.reorderablelist.MonarchReorderableListLabels

/**
 * Replacements for the built-in strings of a `MonarchApprovalChainEditor`. Every `null` keeps the built-in
 * string `monarch_approvalchaineditor_<key>`.
 *
 * @property title the heading of the list, "Steps, in order".
 * @property step the visible heading of each step, from its position: "Step 1".
 * @property approver the label of the approver menu, "Approver".
 * @property target the label of a target picker when the kind has no `targetLabel`, "Choose".
 * @property targetPlaceholder shown in a target picker while nothing is chosen, "Choose…".
 * @property label the label of the optional name field, "Label (optional)".
 * @property labelPlaceholder the placeholder of the name field, "e.g. Finance check".
 * @property addStep the add button, "Add step".
 * @property remove the visible text of a remove button without an icon, "Remove".
 * @property removeStep the TalkBack name of a remove button, from the position: "Remove step 1".
 * @property targetRequired the error of a target picker with nothing chosen.
 * @property maxReached shown instead of the add button at the limit, from the limit.
 * @property reorder the strings of the reorderable list: the handle, move buttons and announcements.
 */
@Immutable
public class MonarchApprovalChainEditorLabels(
    public val title: String? = null,
    public val step: ((position: Int) -> String)? = null,
    public val approver: String? = null,
    public val target: String? = null,
    public val targetPlaceholder: String? = null,
    public val label: String? = null,
    public val labelPlaceholder: String? = null,
    public val addStep: String? = null,
    public val remove: String? = null,
    public val removeStep: ((position: Int) -> String)? = null,
    public val targetRequired: String? = null,
    public val maxReached: ((max: Int) -> String)? = null,
    public val reorder: MonarchReorderableListLabels = MonarchReorderableListLabels(),
)
