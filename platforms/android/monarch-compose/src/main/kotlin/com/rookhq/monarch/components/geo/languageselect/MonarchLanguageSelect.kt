package com.rookhq.monarch.components.geo.languageselect

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import com.rookhq.monarch.R
import com.rookhq.monarch.components.forms.combobox.MonarchComboboxPresentation
import com.rookhq.monarch.components.geo.countryselect.CountrySelectField
import com.rookhq.monarch.components.geo.countryselect.CountrySelectFieldTexts
import com.rookhq.monarch.components.geo.countryselect.countrySelectComboboxLabels
import com.rookhq.monarch.internal.monarchString
import java.util.Locale

/**
 * Chooses a language by its BCP 47 tag from a list the person can search,
 * each language under its own name.
 *
 * Built on `MonarchCombobox`, so it is the Material 3 exposed dropdown field
 * that opens a bottom sheet with a search field for the full list. Names come
 * from `Locale.getDisplayName`: by default each row shows the language's own
 * name ("Deutsch") with its name in [locale] ("German") as a second line, so
 * people find their language whatever the interface language is. The list is
 * sorted by the name in [locale]. Typing matches every word against both
 * names, the tag and the English name. [priority] languages are listed first
 * under "Suggested" while nothing is typed.
 *
 * @param selected the chosen BCP 47 tag, such as "pt-BR", or `null`. It is compared in its canonical form.
 * @param onSelectedChange called with the chosen canonical tag, or `null` when the selection is cleared.
 * @param modifier the modifier of the field.
 * @param locale the interface language: the second name of each row and the sort order use it. `null` uses the first
 * locale of the configuration.
 * @param languages limits the list to these tags, such as the languages the product is translated into. `null` lists
 * about 80 widely used languages.
 * @param priority tags listed first under "Suggested" while nothing is typed, such as the device's languages.
 * @param labelStyle which name each row shows first.
 * @param presentation a menu, a sheet, or automatic: a menu for up to eight languages, a sheet otherwise.
 * @param enabled whether the list can be opened.
 * @param showTag shows the tag at the end of each row.
 * @param placeholder shown in the empty field while nothing is chosen.
 * @param helperText guidance below the field.
 * @param errorText the error message; the field is in the error state.
 * @param clearOptionLabel adds a first row that clears the selection.
 * @param clearIcon the icon of a clear button at the end of the field while a language is chosen.
 * @param labels replacements for the built-in strings.
 */
@Composable
@Suppress("LongParameterList")
public fun MonarchLanguageSelect(
    selected: String?,
    onSelectedChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
    locale: Locale? = null,
    languages: List<String>? = null,
    priority: List<String> = emptyList(),
    labelStyle: MonarchLanguageSelectLabelStyle = MonarchLanguageSelectLabelStyle.Endonym,
    presentation: MonarchComboboxPresentation = MonarchComboboxPresentation.Automatic,
    enabled: Boolean = true,
    showTag: Boolean = false,
    placeholder: String? = null,
    helperText: String? = null,
    errorText: String? = null,
    clearOptionLabel: String? = null,
    clearIcon: (@Composable () -> Unit)? = null,
    labels: MonarchLanguageSelectLabels = MonarchLanguageSelectLabels(),
) {
    val shownLocale = locale ?: LocalConfiguration.current.locales[0]
    val value = selected?.let { LanguageSelectData.canonical(it) ?: it }
    val options = remember(shownLocale, languages, value, labelStyle, showTag) {
        LanguageSelectData.options(languages, value, shownLocale, labelStyle, showTag)
    }
    CountrySelectField(
        options = options,
        selected = value,
        onSelectedChange = onSelectedChange,
        priority = priority.mapNotNull(LanguageSelectData::canonical),
        texts = CountrySelectFieldTexts(
            label = monarchString(labels.label, R.string.monarch_languageselect_label),
            suggested = monarchString(labels.suggested, R.string.monarch_languageselect_suggested),
            all = monarchString(labels.all, R.string.monarch_languageselect_all),
            placeholder = placeholder,
            helperText = helperText,
            errorText = errorText,
            clearOptionLabel = clearOptionLabel,
            combobox = countrySelectComboboxLabels(
                labels.combobox,
                monarchString(labels.empty, R.string.monarch_languageselect_empty),
            ),
        ),
        modifier = modifier,
        presentation = presentation,
        enabled = enabled,
        sortByMatch = true,
        clearIcon = clearIcon,
    )
}
