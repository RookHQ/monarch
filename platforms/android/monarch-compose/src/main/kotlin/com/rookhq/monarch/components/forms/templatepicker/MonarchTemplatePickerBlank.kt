package com.rookhq.monarch.components.forms.templatepicker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

/**
 * The "start from scratch" card of a `MonarchTemplatePicker`, shown first
 * whatever the filters.
 *
 * @property id the id passed to `onSelect` when it is chosen.
 * @property name the card's name. `null` uses the built-in "Blank" (`monarch_templatepicker_blank`).
 * @property description a line under the name, such as "Start with an empty page".
 * @property preview a thumbnail drawn by the app, hidden from TalkBack.
 */
@Immutable
public class MonarchTemplatePickerBlank(
    public val id: String = "blank",
    public val name: String? = null,
    public val description: String? = null,
    public val preview: (@Composable () -> Unit)? = null,
)
