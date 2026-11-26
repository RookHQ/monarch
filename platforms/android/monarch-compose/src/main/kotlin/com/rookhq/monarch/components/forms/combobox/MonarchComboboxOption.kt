package com.rookhq.monarch.components.forms.combobox

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import java.util.Locale

/**
 * One choice of a `MonarchCombobox`.
 *
 * @property id the stable id, passed to `onSelectedChange` and compared with `selected`.
 * @property label the visible name, shown in the list and in the field once chosen. Already localised.
 * @property description a second line under the label.
 * @property hint short text at the end of the row, such as a count, a code or an email. Matched by the default
 * filter.
 * @property group the heading the option is listed under. Consecutive options with the same group share one
 * heading.
 * @property keywords extra words the default filter matches but never shows, such as old names or aliases.
 * @property enabled whether the option can be chosen. A disabled option stays visible in the disabled colour.
 * @property leading content before the label in the list, such as a flag, an avatar or a colour swatch. It is
 * decoration: the label names the option.
 * @property labelLocale the language the label is written in, when it differs from the app's, such as the
 * endonym "Deutsch" in a language picker. TalkBack then reads the label in that language.
 */
@Immutable
public class MonarchComboboxOption(
    public val id: String,
    public val label: String,
    public val description: String? = null,
    public val hint: String? = null,
    public val group: String? = null,
    public val keywords: String? = null,
    public val enabled: Boolean = true,
    public val leading: (@Composable () -> Unit)? = null,
    public val labelLocale: Locale? = null,
)
