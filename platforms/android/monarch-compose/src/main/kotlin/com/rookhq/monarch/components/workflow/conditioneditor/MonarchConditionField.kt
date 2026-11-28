package com.rookhq.monarch.components.workflow.conditioneditor

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.forms.select.MonarchSelectOption

/**
 * A field a condition can test, such as "Priority" or "Hours since created".
 *
 * @property key the stable key stored on the condition, such as `priority` or `requester.email`.
 * @property label the visible name, already localised.
 * @property type the kind of value; it decides the operators and the value input.
 * @property options the choices of a `Select` or `MultiSelect` field, and the suggestions of a `Tags` field.
 * @property group the heading the field is listed under in the field menu, such as "Custom fields".
 * @property operators the operators to offer instead of the defaults for the type.
 * @property placeholder the placeholder of a text or number value, such as "Hours".
 * @property enabled whether the field can be chosen. A disabled field is still listed, so a stored condition
 * keeps its name.
 * @property disabledReason a short line under a disabled field in the menu, such as "comes after this question".
 */
@Immutable
public class MonarchConditionField(
    public val key: String,
    public val label: String,
    public val type: MonarchConditionFieldType,
    public val options: List<MonarchSelectOption<String>> = emptyList(),
    public val group: String? = null,
    public val operators: List<MonarchConditionOperator>? = null,
    public val placeholder: String? = null,
    public val enabled: Boolean = true,
    public val disabledReason: String? = null,
)
