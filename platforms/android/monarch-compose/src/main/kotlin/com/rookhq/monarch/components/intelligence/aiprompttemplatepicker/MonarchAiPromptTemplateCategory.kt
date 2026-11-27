package com.rookhq.monarch.components.intelligence.aiprompttemplatepicker

import androidx.compose.runtime.Immutable

/**
 * A category that `MonarchAiPromptTemplatePicker` offers as a filter.
 *
 * @property id the id that templates name in their `category`.
 * @property label the name of the filter. Already localised.
 */
@Immutable
public data class MonarchAiPromptTemplateCategory(
    public val id: String,
    public val label: String,
)
