package dev.monarchui.components.workflow.approvalchaineditor

import android.content.res.Resources
import dev.monarchui.R

/** The strings of a MonarchApprovalChainEditor: each override from [labels], or the built-in resource. */
internal class MonarchApprovalChainEditorText(
    private val labels: MonarchApprovalChainEditorLabels,
    private val resources: Resources,
) {
    private fun string(override: String?, id: Int): String = override ?: resources.getString(id)

    val title: String get() = string(labels.title, R.string.monarch_approvalchaineditor_title)
    val approver: String get() = string(labels.approver, R.string.monarch_approvalchaineditor_approver)
    val target: String get() = string(labels.target, R.string.monarch_approvalchaineditor_target)
    val targetPlaceholder: String
        get() = string(labels.targetPlaceholder, R.string.monarch_approvalchaineditor_target_placeholder)
    val label: String get() = string(labels.label, R.string.monarch_approvalchaineditor_label)
    val labelPlaceholder: String
        get() = string(labels.labelPlaceholder, R.string.monarch_approvalchaineditor_label_placeholder)
    val addStep: String get() = string(labels.addStep, R.string.monarch_approvalchaineditor_add_step)
    val remove: String get() = string(labels.remove, R.string.monarch_approvalchaineditor_remove)
    val targetRequired: String
        get() = string(labels.targetRequired, R.string.monarch_approvalchaineditor_target_required)

    fun step(position: Int): String =
        labels.step?.invoke(position) ?: resources.getString(R.string.monarch_approvalchaineditor_step, position)

    fun removeStep(position: Int): String = labels.removeStep?.invoke(position)
        ?: resources.getString(R.string.monarch_approvalchaineditor_remove_step, position)

    fun maxReached(max: Int): String = labels.maxReached?.invoke(max)
        ?: resources.getQuantityString(R.plurals.monarch_approvalchaineditor_max_reached, max, max)
}
