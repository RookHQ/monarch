package dev.monarchui.components.workflow.escalationpolicyeditor

import android.content.res.Resources
import dev.monarchui.R

/** The strings of a MonarchEscalationPolicyEditor: each override from [labels], or the built-in resource. */
internal class MonarchEscalationPolicyEditorText(
    private val labels: MonarchEscalationPolicyEditorLabels,
    private val resources: Resources,
) {
    private fun string(override: String?, id: Int): String = override ?: resources.getString(id)

    val label: String get() = string(labels.label, R.string.monarch_escalationpolicyeditor_label)
    val after: String get() = string(labels.after, R.string.monarch_escalationpolicyeditor_after)
    val notify: String get() = string(labels.notify, R.string.monarch_escalationpolicyeditor_notify)
    val nobody: String get() = string(labels.nobody, R.string.monarch_escalationpolicyeditor_nobody)
    val remove: String get() = string(labels.remove, R.string.monarch_escalationpolicyeditor_remove)
    val add: String get() = string(labels.add, R.string.monarch_escalationpolicyeditor_add)
    val noTargets: String get() = string(labels.noTargets, R.string.monarch_escalationpolicyeditor_no_targets)

    fun level(position: Int): String =
        labels.level?.invoke(position) ?: resources.getString(R.string.monarch_escalationpolicyeditor_level, position)

    fun removeLevel(position: Int): String = labels.removeLevel?.invoke(position)
        ?: resources.getString(R.string.monarch_escalationpolicyeditor_remove_level, position)

    fun summary(duration: String, names: String): String = labels.summary?.invoke(duration, names)
        ?: resources.getString(R.string.monarch_escalationpolicyeditor_summary, duration, names)

    fun problem(problem: MonarchEscalationLevelProblem): String = when (problem) {
        MonarchEscalationLevelProblem.Targets ->
            string(labels.targetsProblem, R.string.monarch_escalationpolicyeditor_targets_problem)
        MonarchEscalationLevelProblem.Order ->
            string(labels.orderProblem, R.string.monarch_escalationpolicyeditor_order_problem)
    }

    fun maxReached(max: Int): String = labels.maxReached?.invoke(max)
        ?: resources.getQuantityString(R.plurals.monarch_escalationpolicyeditor_max_reached, max, max)
}
