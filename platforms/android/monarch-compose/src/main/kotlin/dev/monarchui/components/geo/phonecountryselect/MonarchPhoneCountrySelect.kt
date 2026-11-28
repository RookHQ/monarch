package dev.monarchui.components.geo.phonecountryselect

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import dev.monarchui.R
import dev.monarchui.components.forms.combobox.ComboboxFilter
import dev.monarchui.components.forms.combobox.MonarchComboboxPresentation
import dev.monarchui.components.geo.countryselect.CountrySelectData
import dev.monarchui.components.geo.countryselect.CountrySelectField
import dev.monarchui.components.geo.countryselect.CountrySelectFieldTexts
import dev.monarchui.components.geo.countryselect.countrySelectComboboxLabels
import dev.monarchui.internal.monarchString
import java.util.Locale

/**
 * Chooses the country calling code of a phone number from a list of
 * countries the person can search.
 *
 * Built on `MonarchCombobox`, so it is the Material 3 exposed dropdown field
 * that opens a bottom sheet with a search field. Android exposes no calling
 * codes, so the ITU-T E.164 table is bundled; names come from
 * `Locale.getDisplayCountry` in [locale]. Each row reads "Portugal (+351)",
 * sorted by that text. Typing matches the name, the ISO code or the digits
 * ("44", "+44"). [preferred] countries are listed first under "Suggested"
 * while nothing is typed. The selection cannot be cleared unless
 * [clearOptionLabel] is given: a phone number always needs a country.
 *
 * @param selected the chosen ISO 3166-1 alpha-2 code, such as "GB", in any case, or `null`.
 * @param onSelectedChange called with the chosen upper-case code and its calling code ("+44"), or two `null`s when
 * the selection is cleared.
 * @param modifier the modifier of the field.
 * @param locale the language of the country names and of the sort order. `null` uses the first locale of the
 * configuration.
 * @param countries limits the list to these codes. `null` lists every country with a calling code.
 * @param preferred codes listed first under "Suggested" while nothing is typed, such as the person's own country.
 * @param presentation a menu, a sheet, or automatic: a menu for up to eight countries, a sheet otherwise.
 * @param enabled whether the list can be opened.
 * @param showFlag shows the flag emoji at the end of each row. Off by default: TalkBack reads a flag emoji as "Flag:"
 * and the country's name, which repeats the name.
 * @param placeholder shown in the empty field while nothing is chosen.
 * @param helperText guidance below the field.
 * @param errorText the error message; the field is in the error state.
 * @param clearOptionLabel adds a first row that clears the selection.
 * @param labels replacements for the built-in strings.
 * @param labelVisible whether the label shows above the field; false keeps it as the spoken name.
 */
@Composable
@Suppress("LongParameterList")
public fun MonarchPhoneCountrySelect(
    selected: String?,
    onSelectedChange: (code: String?, dialCode: String?) -> Unit,
    modifier: Modifier = Modifier,
    locale: Locale? = null,
    countries: List<String>? = null,
    preferred: List<String> = emptyList(),
    presentation: MonarchComboboxPresentation = MonarchComboboxPresentation.Automatic,
    enabled: Boolean = true,
    showFlag: Boolean = false,
    placeholder: String? = null,
    helperText: String? = null,
    errorText: String? = null,
    clearOptionLabel: String? = null,
    labels: MonarchPhoneCountrySelectLabels = MonarchPhoneCountrySelectLabels(),
    labelVisible: Boolean = true,
) {
    val shownLocale = locale ?: LocalConfiguration.current.locales[0]
    val template = stringResource(R.string.monarch_phonecountryselect_option)
    val optionText = labels.option ?: { name, dialCode -> String.format(shownLocale, template, name, dialCode) }
    val options = remember(shownLocale, countries, showFlag, template, labels.option) {
        PhoneCountrySelectData.options(countries, shownLocale, showFlag, optionText)
    }
    CountrySelectField(
        options = options,
        selected = selected?.let(CountrySelectData::canonical)?.ifEmpty { null },
        onSelectedChange = { code ->
            onSelectedChange(code, code?.let { PhoneCountrySelectData.country(it)?.dialCode })
        },
        priority = preferred.map(CountrySelectData::canonical),
        texts = CountrySelectFieldTexts(
            label = monarchString(labels.label, R.string.monarch_phonecountryselect_label),
            suggested = monarchString(labels.suggested, R.string.monarch_phonecountryselect_suggested),
            all = monarchString(labels.all, R.string.monarch_phonecountryselect_all),
            placeholder = placeholder,
            helperText = helperText,
            errorText = errorText,
            clearOptionLabel = clearOptionLabel,
            combobox = countrySelectComboboxLabels(
                labels.combobox,
                monarchString(labels.empty, R.string.monarch_phonecountryselect_empty),
            ),
            labelVisible = labelVisible,
        ),
        modifier = modifier,
        presentation = presentation,
        enabled = enabled,
        sortByMatch = true,
        clearIcon = null,
        filter = ComboboxFilter::matches,
    )
}
