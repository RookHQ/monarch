package dev.monarchui.components.workflow.conditioneditor

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchConditionEditor`. Every `null` keeps the built-in string
 * `monarch_conditioneditor_<key>`; the maps replace only the entries they name.
 *
 * @property label the TalkBack name of the list, "Conditions".
 * @property condition the heading of one condition, from its position: "Condition 1".
 * @property field the label of the field menu, "Field".
 * @property operator the label of the operator menu, "Operator".
 * @property value the label of the value input, "Value".
 * @property remove the visible text of the remove button, "Remove", used when no remove icon is given.
 * @property removeItem the TalkBack name of the remove button, from the position: "Remove condition 1".
 * @property add the add button, "Add condition".
 * @property matchBefore the text before the all / any switch, "Match".
 * @property matchAfter the text after it, "of these conditions".
 * @property matchAll the "all" segment.
 * @property matchAny the "any" segment.
 * @property empty shown when there are no conditions.
 * @property choose the placeholder of a menu with nothing chosen, "Choose…".
 * @property operators the visible names of operators.
 * @property unknownField the name of a stored field the list no longer has, from its key.
 * @property removedOption the name of a stored value no longer among the options, from the value.
 * @property problems the message of each problem.
 * @property maxReached shown instead of the add button at the limit, from the limit.
 * @property chooseDate the date button while no day is chosen.
 * @property confirmDate the confirm button of the date picker dialog.
 * @property cancel the button that closes the date picker dialog.
 */
@Immutable
public class MonarchConditionEditorLabels(
    public val label: String? = null,
    public val condition: ((position: Int) -> String)? = null,
    public val field: String? = null,
    public val operator: String? = null,
    public val value: String? = null,
    public val remove: String? = null,
    public val removeItem: ((position: Int) -> String)? = null,
    public val add: String? = null,
    public val matchBefore: String? = null,
    public val matchAfter: String? = null,
    public val matchAll: String? = null,
    public val matchAny: String? = null,
    public val empty: String? = null,
    public val choose: String? = null,
    public val operators: Map<MonarchConditionOperator, String> = emptyMap(),
    public val unknownField: ((key: String) -> String)? = null,
    public val removedOption: ((value: String) -> String)? = null,
    public val problems: Map<MonarchConditionProblem, String> = emptyMap(),
    public val maxReached: ((max: Int) -> String)? = null,
    public val chooseDate: String? = null,
    public val confirmDate: String? = null,
    public val cancel: String? = null,
)
