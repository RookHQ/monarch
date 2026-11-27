package com.rookhq.monarch.components.intelligence.aiprompttemplatepicker

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchAiPromptTemplatePicker`.
 * Every `null` keeps the built-in string `monarch_aiprompttemplatepicker_<key>`.
 *
 * @property title the heading of the gallery, "Prompt templates".
 * @property search the placeholder of the search field, "Search templates".
 * @property allCategories the filter that shows every category, "All".
 * @property template the heading of a template's text when it has no blanks, "Template".
 * @property preview the heading of the filled prompt, "Prompt preview".
 * @property use the button that hands the prompt to the app, "Use template".
 * @property cancel the button that closes the picker, "Cancel".
 * @property back the button that goes back to the gallery, "All templates".
 * @property optional added to the label of a field that may stay empty, "Optional".
 * @property missing the error of an empty required field, from its label: "Enter customer name.".
 * @property empty the title when there are no templates, "No templates yet".
 * @property noResults the title when the search finds nothing, from the query: "No templates match “tone”".
 */
@Immutable
public class MonarchAiPromptTemplatePickerLabels(
    public val title: String? = null,
    public val search: String? = null,
    public val allCategories: String? = null,
    public val template: String? = null,
    public val preview: String? = null,
    public val use: String? = null,
    public val cancel: String? = null,
    public val back: String? = null,
    public val optional: String? = null,
    public val missing: ((label: String) -> String)? = null,
    public val empty: String? = null,
    public val noResults: ((query: String) -> String)? = null,
)
