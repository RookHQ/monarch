package com.rookhq.monarch.components.workflow.escalationpolicyeditor

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchEscalationPolicyEditor`. Every `null` keeps the built-in
 * string `monarch_escalationpolicyeditor_<key>`.
 *
 * @property label the TalkBack name of the list of levels, "Escalation levels".
 * @property level the heading of each level, from its position: "Level 1".
 * @property after the label of the time field, "After".
 * @property notify the label of the target picker, "Notify".
 * @property summary the line under each heading, from the duration ("15m") and the names notified.
 * @property nobody the names in the summary while a level notifies nobody, "nobody yet".
 * @property remove the visible text of a remove button without an icon, "Remove".
 * @property removeLevel the TalkBack name of a remove button, from the position: "Remove level 1".
 * @property add the add button, "Add level".
 * @property noTargets the warning shown when there is nothing to notify.
 * @property targetsProblem the error of a level that notifies nobody.
 * @property orderProblem the error of a level that is not later than the one before.
 * @property maxReached shown instead of the add button at the limit, from the limit.
 */
@Immutable
public class MonarchEscalationPolicyEditorLabels(
    public val label: String? = null,
    public val level: ((position: Int) -> String)? = null,
    public val after: String? = null,
    public val notify: String? = null,
    public val summary: ((duration: String, names: String) -> String)? = null,
    public val nobody: String? = null,
    public val remove: String? = null,
    public val removeLevel: ((position: Int) -> String)? = null,
    public val add: String? = null,
    public val noTargets: String? = null,
    public val targetsProblem: String? = null,
    public val orderProblem: String? = null,
    public val maxReached: ((max: Int) -> String)? = null,
)
