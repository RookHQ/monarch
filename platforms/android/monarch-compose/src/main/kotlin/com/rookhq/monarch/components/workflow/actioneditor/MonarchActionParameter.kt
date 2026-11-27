package com.rookhq.monarch.components.workflow.actioneditor

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.forms.select.MonarchSelectOption

/**
 * One input of a [MonarchActionType], such as "Team" for "Set team".
 *
 * @property key the key of the value in the action's parameters.
 * @property label the visible label, already localised.
 * @property kind the kind of input.
 * @property options the choices of `Select` and `MultiSelect`; the suggestions of `Tags`.
 * @property required whether the action is incomplete while this is empty.
 * @property description help under the input.
 * @property placeholder the placeholder of the input.
 * @property emptyLabel the empty choice of an optional `Select`, such as "The ticket's current team". "Choose…"
 * when `null`.
 * @property min the smallest allowed number of a `Number`.
 * @property max the largest allowed number of a `Number`.
 * @property visibleWhen shows the input only while this holds, such as "Agent" only when "Notify" is "A specific
 * agent". A hidden input is not validated.
 */
@Immutable
public class MonarchActionParameter(
    public val key: String,
    public val label: String,
    public val kind: MonarchActionParameterKind,
    public val options: List<MonarchSelectOption<String>> = emptyList(),
    public val required: Boolean = false,
    public val description: String? = null,
    public val placeholder: String? = null,
    public val emptyLabel: String? = null,
    public val min: Double? = null,
    public val max: Double? = null,
    public val visibleWhen: ((parameters: Map<String, MonarchActionValue>) -> Boolean)? = null,
) {
    /** Whether the input shows for [parameters]. */
    public fun isVisible(parameters: Map<String, MonarchActionValue>): Boolean = visibleWhen?.invoke(parameters) ?: true
}
