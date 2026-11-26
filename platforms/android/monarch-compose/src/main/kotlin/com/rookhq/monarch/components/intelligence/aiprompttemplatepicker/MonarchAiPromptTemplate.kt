package com.rookhq.monarch.components.intelligence.aiprompttemplatepicker

import androidx.compose.runtime.Immutable

/**
 * A reusable prompt for `MonarchAiPromptTemplatePicker`.
 *
 * @property id a stable id.
 * @property title a short name. Already localised.
 * @property body the prompt, with `{{name}}` blanks.
 * @property description what the template is for.
 * @property category the id of a [MonarchAiPromptTemplateCategory].
 * @property keywords extra words the search matches.
 * @property variables the blanks, in form order. `null` reads them from [body], all required.
 */
@Immutable
public data class MonarchAiPromptTemplate(
    public val id: String,
    public val title: String,
    public val body: String,
    public val description: String? = null,
    public val category: String? = null,
    public val keywords: List<String> = emptyList(),
    public val variables: List<MonarchAiPromptTemplateVariable>? = null,
) {
    /**
     * The blanks of the template: its [variables], or the ones read from the
     * body, all required, with labels made from their names: `customer_name`
     * becomes "Customer name".
     */
    public val resolvedVariables: List<MonarchAiPromptTemplateVariable>
        get() = variables ?: variableNames(body).map {
            MonarchAiPromptTemplateVariable(
                name = it,
                label = MonarchAiPromptTemplatePickerTokens.humanize(it),
                required = true,
            )
        }

    /** Reads and fills the blanks of a body. */
    public companion object {
        /** The distinct `{{name}}` blanks of [body], in order of first use. Spaces inside the braces are allowed. */
        public fun variableNames(body: String): List<String> =
            MonarchAiPromptTemplatePickerTokens.Blank.findAll(body).map { it.groupValues[1] }.distinct().toList()

        /**
         * Fills [body]: each `{{name}}` becomes its value in [values]. A blank
         * without a value is left as it is, so nothing is dropped silently; an
         * empty value removes the blank.
         */
        public fun fill(body: String, values: Map<String, String>): String =
            MonarchAiPromptTemplatePickerTokens.Blank.replace(body) { match ->
                values[match.groupValues[1]] ?: match.value
            }
    }
}
