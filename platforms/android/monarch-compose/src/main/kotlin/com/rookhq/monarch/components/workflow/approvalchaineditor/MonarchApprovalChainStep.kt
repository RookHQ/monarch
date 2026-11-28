package com.rookhq.monarch.components.workflow.approvalchaineditor

import androidx.compose.runtime.Immutable

/**
 * One step of an approval chain, the value of a `MonarchApprovalChainEditor`.
 *
 * @property id a stable, unique id.
 * @property approver the `value` of a [MonarchApprovalApproverType].
 * @property target the chosen target (a role key, a person id) for kinds with targets, or `null`.
 * @property label an optional name shown in timelines, such as "Finance check".
 */
@Immutable
public data class MonarchApprovalChainStep(
    public val id: String,
    public val approver: String,
    public val target: String? = null,
    public val label: String? = null,
) {
    /** This step after its approver kind changed: a target the new kind does not offer is dropped. */
    internal fun cleaned(approverTypes: List<MonarchApprovalApproverType>): MonarchApprovalChainStep {
        val type = approverTypes.firstOrNull { it.value == approver }
        val keep = target != null && type?.targets?.any { it.id == target } == true
        return copy(target = target.takeIf { keep }, label = label?.takeIf { it.isNotEmpty() })
    }

    public companion object {
        /**
         * The ids of the steps whose approver kind has targets but none of them is chosen. Empty when the chain
         * is complete.
         */
        public fun validate(
            steps: List<MonarchApprovalChainStep>,
            approverTypes: List<MonarchApprovalApproverType>,
        ): Set<String> = steps.filter { step ->
            val targets = approverTypes.firstOrNull { it.value == step.approver }?.targets
            targets != null && targets.none { it.id == step.target }
        }.mapTo(LinkedHashSet()) { it.id }

        /**
         * A plain-text summary of a chain, such as "Reporting manager → Finance check (Role: Finance team)", for
         * list rows and confirmation text.
         */
        public fun summarize(
            steps: List<MonarchApprovalChainStep>,
            approverTypes: List<MonarchApprovalApproverType>,
            separator: String = " → ",
        ): String = steps.joinToString(separator) { summary(it, approverTypes) }

        /** One step as text: the kind and target, after the step's label when it has one. */
        internal fun summary(step: MonarchApprovalChainStep, approverTypes: List<MonarchApprovalApproverType>): String {
            val type = approverTypes.firstOrNull { it.value == step.approver }
            val target = type?.targets?.firstOrNull { it.id == step.target }?.name
            val base = when {
                type == null -> step.approver
                target != null -> "${type.label}: $target"
                else -> type.label
            }
            return if (step.label.isNullOrEmpty()) base else "${step.label} ($base)"
        }
    }
}
