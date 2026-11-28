package dev.monarchui.components.workflow.approvalchaineditor

import androidx.compose.runtime.Immutable
import dev.monarchui.components.forms.entitypicker.MonarchEntityPickerEntity

/**
 * A kind of approver a step of a `MonarchApprovalChainEditor` can name, such as "Reporting manager" or "Anyone
 * with a role".
 *
 * @property value the stable value stored on the step.
 * @property label the visible name in the approver menu.
 * @property targets when the kind needs a choice (which role, which person), the entities to choose from. A step
 * of this kind is incomplete until one is chosen. `null` when the kind needs no choice.
 * @property targetLabel the label of the target picker, such as "Role" or "Person". `null` uses the editor's
 * label, "Choose".
 */
@Immutable
public class MonarchApprovalApproverType(
    public val value: String,
    public val label: String,
    public val targets: List<MonarchEntityPickerEntity>? = null,
    public val targetLabel: String? = null,
)
