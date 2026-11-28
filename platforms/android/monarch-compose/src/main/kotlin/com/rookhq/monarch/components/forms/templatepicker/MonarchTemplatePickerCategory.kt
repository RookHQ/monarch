package com.rookhq.monarch.components.forms.templatepicker

import androidx.compose.runtime.Immutable

/**
 * A category filter of a `MonarchTemplatePicker`.
 *
 * @property id the id that templates name in `MonarchTemplatePickerItem.category`.
 * @property label the filter's text, already localised.
 */
@Immutable
public class MonarchTemplatePickerCategory(public val id: String, public val label: String)
