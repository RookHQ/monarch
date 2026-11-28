package com.rookhq.monarch.components.geo.locationfields

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.rookhq.monarch.R
import com.rookhq.monarch.components.forms.textfield.MonarchTextField
import com.rookhq.monarch.components.geo.countryselect.MonarchCountrySelect
import com.rookhq.monarch.components.geo.countryselect.MonarchCountrySelectLabels
import com.rookhq.monarch.components.geo.regionselect.MonarchRegionSelect
import com.rookhq.monarch.components.geo.regionselect.MonarchRegionSelectLabels
import com.rookhq.monarch.components.geo.regionselect.MonarchRegionSelectRegion
import com.rookhq.monarch.components.geo.regionselect.RegionSelectData
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.internal.monarchString
import java.util.Locale

/**
 * Country, region, city and postal code as one address block, with the
 * lists supplied by the app.
 *
 * Built from Monarch fields over Material 3: `MonarchCountrySelect` for the
 * country, `MonarchRegionSelect` for the region (a list under the country's
 * own word, "State" or "Province or territory", or a text field for a country
 * without a list), a `MonarchCombobox` sheet that searches
 * [MonarchLocationAdapter.cities] as the person types, and `MonarchTextField`s
 * for a city without a source and for the postal code. The fields stack in
 * one column, as address forms do on a phone, and each one carries its
 * autofill type.
 *
 * Picking a country clears the other parts; picking a region clears the
 * city. Cities are searched [cityDebounceMillis] after the last keystroke,
 * from [cityMinQuery] letters while no region is chosen; a chosen region's
 * cities are listed at once. A city not in the list can be kept as typed
 * ([allowCustomCity]), and a city chosen before its region fills the region.
 *
 * @param value the address. The app keeps it and passes it back.
 * @param onValueChange called with the whole new address and the part that changed.
 * @param modifier the modifier of the column.
 * @param adapter where the country, region and city lists come from.
 * @param fields the parts to show, in order. All four by default.
 * @param errors an error message per part; that field is in the error state and TalkBack reads the message with it.
 * @param enabled whether the fields can be used.
 * @param legend a heading above the fields, such as "Shipping address".
 * @param locale the language of the country names and their order. `null` uses the configuration's first locale.
 * @param cityMinQuery the letters needed before cities are searched while no region is chosen.
 * @param cityDebounceMillis how long to wait after the last keystroke before cities are searched.
 * @param allowCustomCity offers the typed text as the city when it is not in the list.
 * @param labels replacements for the built-in strings.
 * @param required the fields that show the required mark after their label.
 */
@Composable
@Suppress("LongParameterList")
public fun MonarchLocationFields(
    value: MonarchLocationValue,
    onValueChange: (value: MonarchLocationValue, changed: MonarchLocationField) -> Unit,
    modifier: Modifier = Modifier,
    adapter: MonarchLocationAdapter = MonarchLocationAdapter(),
    fields: List<MonarchLocationField> = MonarchLocationField.entries,
    errors: Map<MonarchLocationField, String> = emptyMap(),
    enabled: Boolean = true,
    legend: String? = null,
    locale: Locale? = null,
    cityMinQuery: Int = MonarchLocationFieldsTokens.DEFAULT_CITY_MIN_QUERY,
    cityDebounceMillis: Long = MonarchLocationFieldsTokens.DEFAULT_CITY_DEBOUNCE_MILLIS,
    allowCustomCity: Boolean = true,
    labels: MonarchLocationFieldsLabels = MonarchLocationFieldsLabels(),
    required: Set<MonarchLocationField> = emptySet(),
) {
    val current by rememberUpdatedState(value)
    val change by rememberUpdatedState(onValueChange)
    // The regions each country loaded, so a chosen code can be told apart from typed text.
    val loaded = remember { mutableStateMapOf<String, List<MonarchRegionSelectRegion>>() }
    Column(
        modifier = modifier.semantics { isTraversalGroup = true },
        verticalArrangement = Arrangement.spacedBy(MonarchLocationFieldsTokens.Gap),
    ) {
        if (legend != null) MonarchText(text = legend, style = MonarchTextStyle.Subheading, heading = true)
        fields.distinct().forEach { field ->
            val error = errors[field]
            when (field) {
                MonarchLocationField.Country -> MonarchCountrySelect(
                    selected = value.country,
                    onSelectedChange = { code ->
                        MonarchLocationFieldsTokens.countryChanged(current, code)?.let { change(it, field) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    locale = locale,
                    countries = adapter.countries,
                    enabled = enabled,
                    errorText = error,
                    labels = MonarchCountrySelectLabels(label = labels.country),
                    required = field in required,
                )
                MonarchLocationField.Region -> LocationFieldsRegion(value, adapter, loaded, error, enabled, labels) {
                    change(it, field)
                }
                MonarchLocationField.City -> LocationFieldsCity(
                    value = value,
                    onChange = { change(it, field) },
                    cities = adapter.cities,
                    config = LocationFieldsCityConfig(
                        label = monarchString(labels.city, R.string.monarch_locationfields_city),
                        errorText = error,
                        enabled = enabled,
                        minQuery = cityMinQuery,
                        debounceMillis = cityDebounceMillis,
                        allowCustom = allowCustomCity,
                        labels = labels,
                        required = field in required,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                MonarchLocationField.PostalCode -> MonarchTextField(
                    value = value.postalCode,
                    onValueChange = { change(current.copy(postalCode = it), field) },
                    modifier = Modifier.fillMaxWidth().semantics { contentType = ContentType.PostalCode },
                    label = monarchString(labels.postalCode, R.string.monarch_locationfields_postal_code),
                    errorText = error,
                    enabled = enabled,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Next,
                    ),
                )
            }
        }
    }
}

/** The region: `MonarchRegionSelect`, with the adapter's loader and word for the country's regions. */
@Composable
@Suppress("LongParameterList")
private fun LocationFieldsRegion(
    value: MonarchLocationValue,
    adapter: MonarchLocationAdapter,
    loaded: MutableMap<String, List<MonarchRegionSelectRegion>>,
    error: String?,
    enabled: Boolean,
    labels: MonarchLocationFieldsLabels,
    onChange: (MonarchLocationValue) -> Unit,
) {
    val country = value.country
    val load = adapter.regions
    val list = country?.let { code ->
        if (load != null) loaded[code] else RegionSelectData.builtIn(code)
    }
    MonarchRegionSelect(
        country = country,
        selected = MonarchLocationFieldsTokens.regionSelection(value),
        onSelectedChange = { selected -> onChange(MonarchLocationFieldsTokens.regionChanged(value, selected, list)) },
        modifier = Modifier.fillMaxWidth(),
        loadRegions = load?.let { source -> { code: String -> source(code).also { loaded[code] = it } } },
        enabled = enabled,
        errorText = error,
        labels = MonarchRegionSelectLabels(label = labels.region ?: country?.let { adapter.regionLabel?.invoke(it) }),
    )
}
