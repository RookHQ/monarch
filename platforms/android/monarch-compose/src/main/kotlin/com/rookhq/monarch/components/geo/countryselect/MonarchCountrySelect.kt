package com.rookhq.monarch.components.geo.countryselect

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import com.rookhq.monarch.R
import com.rookhq.monarch.components.forms.combobox.MonarchComboboxPresentation
import com.rookhq.monarch.internal.monarchString
import java.util.Locale

/**
 * Chooses a country or territory by its ISO 3166-1 alpha-2 code from a list
 * the person can search.
 *
 * Built on `MonarchCombobox`, so it is the Material 3 exposed dropdown field
 * that opens a bottom sheet with a search field for the full list. The 249
 * officially assigned codes are bundled; names come from
 * `Locale.getDisplayCountry` in [locale] and are sorted with that locale's
 * collation. Each row shows the name with the code (and, if asked, the flag
 * emoji) at its end. Typing matches every word against the name, the code
 * and, outside English, the English name, ignoring case and accents.
 * [priority] countries are listed first under "Suggested" while nothing is
 * typed.
 *
 * @param selected the chosen code, such as "PT", in any case, or `null`.
 * @param onSelectedChange called with the chosen upper-case code, or `null` when the selection is cleared.
 * @param modifier the modifier of the field.
 * @param locale the language of the names and of the sort order. `null` uses the first locale of the configuration.
 * @param countries limits the list to these codes, such as the countries the product ships to. `null` lists all 249.
 * @param exclude codes to leave out.
 * @param priority codes listed first under "Suggested" while nothing is typed, such as the detected country.
 * @param presentation a menu, a sheet, or automatic: a menu for up to eight countries, a sheet otherwise.
 * @param enabled whether the list can be opened.
 * @param showCode shows the ISO code at the end of each row.
 * @param showFlag shows the flag emoji at the end of each row. Off by default: TalkBack reads a flag emoji as "Flag:"
 * and the country's name, which repeats the name.
 * @param countryName the app's own name for a code, such as a legal wording; `null` from it keeps the locale's name.
 * @param placeholder shown in the empty field while nothing is chosen.
 * @param helperText guidance below the field.
 * @param errorText the error message; the field is in the error state.
 * @param clearOptionLabel adds a first row that clears the selection, such as "No country".
 * @param clearIcon the icon of a clear button at the end of the field while a country is chosen.
 * @param labels replacements for the built-in strings.
 * @param initiallyOpen opens the list when the field first appears, as the web example does.
 * @param required shows the required mark after the label.
 */
@Composable
@Suppress("LongParameterList")
public fun MonarchCountrySelect(
    selected: String?,
    onSelectedChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
    locale: Locale? = null,
    countries: List<String>? = null,
    exclude: List<String> = emptyList(),
    priority: List<String> = emptyList(),
    presentation: MonarchComboboxPresentation = MonarchComboboxPresentation.Automatic,
    enabled: Boolean = true,
    showCode: Boolean = true,
    showFlag: Boolean = false,
    countryName: ((String) -> String?)? = null,
    placeholder: String? = null,
    helperText: String? = null,
    errorText: String? = null,
    clearOptionLabel: String? = null,
    clearIcon: (@Composable () -> Unit)? = null,
    labels: MonarchCountrySelectLabels = MonarchCountrySelectLabels(),
    initiallyOpen: Boolean = false,
    required: Boolean = false,
) {
    val shownLocale = locale ?: LocalConfiguration.current.locales[0]
    val value = selected?.let(CountrySelectData::canonical)?.ifEmpty { null }
    val options = remember(shownLocale, countries, exclude, value, showCode, showFlag, countryName) {
        val codes = CountrySelectData.codes(countries, exclude)
        CountrySelectData.options(codes, value, shownLocale, CountrySelectStyle(showFlag, showCode, countryName))
    }
    CountrySelectField(
        options = options,
        selected = value,
        onSelectedChange = onSelectedChange,
        priority = priority.map(CountrySelectData::canonical),
        texts = CountrySelectFieldTexts(
            label = monarchString(labels.label, R.string.monarch_countryselect_label),
            suggested = monarchString(labels.suggested, R.string.monarch_countryselect_suggested),
            all = monarchString(labels.all, R.string.monarch_countryselect_all),
            placeholder = placeholder,
            helperText = helperText,
            errorText = errorText,
            clearOptionLabel = clearOptionLabel,
            combobox = countrySelectComboboxLabels(
                labels.combobox,
                monarchString(labels.empty, R.string.monarch_countryselect_empty),
            ),
        ),
        modifier = modifier,
        presentation = presentation,
        enabled = enabled,
        sortByMatch = true,
        clearIcon = clearIcon,
        initiallyOpen = initiallyOpen,
        required = required,
    )
}
