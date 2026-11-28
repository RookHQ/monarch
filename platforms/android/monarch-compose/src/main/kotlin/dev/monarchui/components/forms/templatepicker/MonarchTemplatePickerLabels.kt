package dev.monarchui.components.forms.templatepicker

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchTemplatePicker`. Every
 * `null` keeps the built-in string `monarch_templatepicker_<key>`.
 *
 * @property categories the spoken name of the category filters, "Categories".
 * @property allCategories the filter that shows every category, "All".
 * @property blank the name of the blank card when it has none, "Blank".
 * @property search the placeholder of the search field, "Search templates".
 * @property results the number of templates shown, announced when the filters change: "3 templates".
 * @property noResults the message when the search matches nothing, given the search: "No templates match “x”.".
 * @property emptyCategory the message when a category has no templates, "No templates in this category.".
 */
@Immutable
public class MonarchTemplatePickerLabels(
    public val categories: String? = null,
    public val allCategories: String? = null,
    public val blank: String? = null,
    public val search: String? = null,
    public val results: ((Int) -> String)? = null,
    public val noResults: ((String) -> String)? = null,
    public val emptyCategory: String? = null,
)
