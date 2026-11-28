package com.rookhq.monarch.components.geo.currencyselect

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import com.rookhq.monarch.R
import com.rookhq.monarch.components.forms.combobox.MonarchComboboxPresentation
import com.rookhq.monarch.components.geo.countryselect.CountrySelectData
import com.rookhq.monarch.components.geo.countryselect.CountrySelectField
import com.rookhq.monarch.components.geo.countryselect.CountrySelectFieldTexts
import com.rookhq.monarch.components.geo.countryselect.countrySelectComboboxLabels
import com.rookhq.monarch.internal.monarchString
import java.util.Locale

/**
 * Chooses a currency by its ISO 4217 code from a list the person can search.
 *
 * Built on `MonarchCombobox`, so it is the Material 3 exposed dropdown field
 * that opens a bottom sheet with a search field for the full list. The active
 * ISO 4217 codes are bundled; names and symbols come from `java.util.Currency`
 * in [locale], and the list is sorted by name with that locale's collation.
 * Each row shows the label for [display] with the code and symbol at its end.
 * Typing matches every word against the name, code, symbol and, outside
 * English, the English name. [priority] currencies are listed first under
 * "Suggested" while nothing is typed.
 *
 * @param selected the chosen code, such as "EUR", in any case, or `null`.
 * @param onSelectedChange called with the chosen upper-case code, or `null` when the selection is cleared.
 * @param modifier the modifier of the field.
 * @param locale the language of the names and symbols and of the sort order. `null` uses the first locale of the
 * configuration.
 * @param currencies limits the list to these codes, such as the currencies the payment provider supports. `null` lists
 * every active currency.
 * @param priority codes listed first under "Suggested" while nothing is typed.
 * @param display what the label shows: the name, the code, or both.
 * @param presentation a menu, a sheet, or automatic: a menu for up to eight currencies, a sheet otherwise.
 * @param enabled whether the list can be opened.
 * @param showSymbol shows the symbol next to the code at the end of each row.
 * @param placeholder shown in the empty field while nothing is chosen.
 * @param helperText guidance below the field.
 * @param errorText the error message; the field is in the error state.
 * @param clearOptionLabel adds a first row that clears the selection.
 * @param clearIcon the icon of a clear button at the end of the field while a currency is chosen.
 * @param labels replacements for the built-in strings.
 * @param initiallyOpen opens the list when the field first appears, as the web example does.
 */
@Composable
@Suppress("LongParameterList")
public fun MonarchCurrencySelect(
    selected: String?,
    onSelectedChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
    locale: Locale? = null,
    currencies: List<String>? = null,
    priority: List<String> = emptyList(),
    display: MonarchCurrencySelectDisplay = MonarchCurrencySelectDisplay.Name,
    presentation: MonarchComboboxPresentation = MonarchComboboxPresentation.Automatic,
    enabled: Boolean = true,
    showSymbol: Boolean = true,
    placeholder: String? = null,
    helperText: String? = null,
    errorText: String? = null,
    clearOptionLabel: String? = null,
    clearIcon: (@Composable () -> Unit)? = null,
    labels: MonarchCurrencySelectLabels = MonarchCurrencySelectLabels(),
    initiallyOpen: Boolean = false,
) {
    val shownLocale = locale ?: LocalConfiguration.current.locales[0]
    val value = selected?.let(CountrySelectData::canonical)?.ifEmpty { null }
    val options = remember(shownLocale, currencies, value, display, showSymbol) {
        CurrencySelectData.options(currencies, value, shownLocale, display, showSymbol)
    }
    CountrySelectField(
        options = options,
        selected = value,
        onSelectedChange = onSelectedChange,
        priority = priority.map(CountrySelectData::canonical),
        texts = CountrySelectFieldTexts(
            label = monarchString(labels.label, R.string.monarch_currencyselect_label),
            suggested = monarchString(labels.suggested, R.string.monarch_currencyselect_suggested),
            all = monarchString(labels.all, R.string.monarch_currencyselect_all),
            placeholder = placeholder,
            helperText = helperText,
            errorText = errorText,
            clearOptionLabel = clearOptionLabel,
            combobox = countrySelectComboboxLabels(
                labels.combobox,
                monarchString(labels.empty, R.string.monarch_currencyselect_empty),
            ),
        ),
        modifier = modifier,
        presentation = presentation,
        enabled = enabled,
        sortByMatch = true,
        clearIcon = clearIcon,
        initiallyOpen = initiallyOpen,
    )
}
