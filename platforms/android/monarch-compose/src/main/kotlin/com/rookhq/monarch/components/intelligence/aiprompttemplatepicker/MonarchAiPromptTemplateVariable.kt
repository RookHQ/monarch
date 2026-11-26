package com.rookhq.monarch.components.intelligence.aiprompttemplatepicker

import androidx.compose.runtime.Immutable

/**
 * One `{{name}}` blank of a [MonarchAiPromptTemplate], as a field of the
 * template page of `MonarchAiPromptTemplatePicker`.
 *
 * @property name the name inside the braces.
 * @property label the field label. Already localised.
 * @property required whether Use template asks for a value.
 * @property placeholder the hint inside the empty field.
 * @property helper the help under the field.
 * @property multiline whether the field takes several lines.
 * @property defaultValue the value the field starts with.
 */
@Immutable
public data class MonarchAiPromptTemplateVariable(
    public val name: String,
    public val label: String,
    public val required: Boolean = false,
    public val placeholder: String? = null,
    public val helper: String? = null,
    public val multiline: Boolean = false,
    public val defaultValue: String? = null,
)
