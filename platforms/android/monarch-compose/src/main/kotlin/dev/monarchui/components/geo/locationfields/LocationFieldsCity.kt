package dev.monarchui.components.geo.locationfields

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import dev.monarchui.R
import dev.monarchui.components.forms.combobox.MonarchCombobox
import dev.monarchui.components.forms.combobox.MonarchComboboxLabels
import dev.monarchui.components.forms.combobox.MonarchComboboxPresentation
import dev.monarchui.components.forms.textfield.MonarchTextField
import dev.monarchui.internal.monarchString
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/** The city: a text field without a city source, otherwise a combobox that searches the adapter. */
@Composable
internal fun LocationFieldsCity(
    value: MonarchLocationValue,
    onChange: (MonarchLocationValue) -> Unit,
    cities: (suspend (MonarchLocationCityQuery) -> List<MonarchLocationCity>)?,
    config: LocationFieldsCityConfig,
    modifier: Modifier = Modifier,
) {
    if (cities == null) {
        MonarchTextField(
            value = value.city,
            onValueChange = { onChange(value.copy(city = it, cityId = null)) },
            modifier = modifier.semantics { contentType = ContentType.AddressLocality },
            label = config.label,
            required = config.required,
            errorText = config.errorText,
            enabled = config.enabled,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
            ),
        )
    } else {
        LocationFieldsCitySearch(value, onChange, cities, config, modifier)
    }
}

@Composable
private fun LocationFieldsCitySearch(
    value: MonarchLocationValue,
    onChange: (MonarchLocationValue) -> Unit,
    cities: suspend (MonarchLocationCityQuery) -> List<MonarchLocationCity>,
    config: LocationFieldsCityConfig,
    modifier: Modifier,
) {
    val tokens = MonarchLocationFieldsTokens
    val search = rememberLocationFieldsCitySearch(value.country, value.region, cities, config)
    val noCountry = value.country == null
    MonarchCombobox(
        label = config.label,
        required = config.required,
        options = tokens.cityOptions(search.results, value),
        selected = tokens.citySelection(value),
        onSelectedChange = { id -> tokens.cityChosen(value, id, search.results)?.let(onChange) },
        modifier = modifier,
        presentation = MonarchComboboxPresentation.Sheet,
        enabled = config.enabled && !noCountry,
        loading = search.status == LocationFieldsCityStatus.Loading,
        helperText = if (noCountry) {
            monarchString(config.labels.chooseCountryFirst, R.string.monarch_locationfields_choose_country_first)
        } else {
            null
        },
        errorText = config.errorText,
        query = search.query,
        onQueryChange = search.onQueryChange,
        filter = { _, _ -> true },
        onCreate = if (config.allowCustom && search.status != LocationFieldsCityStatus.Short) {
            tokens::typedOption
        } else {
            null
        },
        labels = locationFieldsCityLabels(search.status, config),
    )
}

/** The state of the city search. */
private class LocationFieldsCitySearchState(
    val query: String,
    val onQueryChange: (String) -> Unit,
    val results: List<MonarchLocationCity>,
    val status: LocationFieldsCityStatus,
)

/** Searches [cities] for the typed text, after the debounce, and again after a failure when the text changes. */
@Composable
private fun rememberLocationFieldsCitySearch(
    country: String?,
    region: String?,
    cities: suspend (MonarchLocationCityQuery) -> List<MonarchLocationCity>,
    config: LocationFieldsCityConfig,
): LocationFieldsCitySearchState {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(emptyList<MonarchLocationCity>()) }
    var status by remember { mutableStateOf(LocationFieldsCityStatus.Idle) }
    var attempt by remember { mutableIntStateOf(0) }
    val search by rememberUpdatedState(cities)
    LaunchedEffect(country, region, query, attempt) {
        val text = query.trim()
        status = when {
            country == null || MonarchLocationFieldsTokens.needsMoreLetters(text, region, config.minQuery) -> {
                results = emptyList()
                if (country == null) LocationFieldsCityStatus.Idle else LocationFieldsCityStatus.Short
            }
            else -> {
                if (text.isNotEmpty()) delay(config.debounceMillis)
                status = LocationFieldsCityStatus.Loading
                try {
                    results = search(MonarchLocationCityQuery(country, region, text))
                    LocationFieldsCityStatus.Idle
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") failed: Exception) {
                    // A failed search is shown in the list; typing again searches again.
                    results = emptyList()
                    LocationFieldsCityStatus.Failed
                }
            }
        }
    }
    return LocationFieldsCitySearchState(
        query = query,
        onQueryChange = {
            query = it
            if (status == LocationFieldsCityStatus.Failed) attempt++
        },
        results = results,
        status = status,
    )
}

/** The combobox strings of the city search, with the list message for [status]. */
@Composable
private fun locationFieldsCityLabels(
    status: LocationFieldsCityStatus,
    config: LocationFieldsCityConfig,
): MonarchComboboxLabels {
    val labels = config.labels
    val empty = when (status) {
        LocationFieldsCityStatus.Failed ->
            monarchString(labels.citiesLoadFailed, R.string.monarch_locationfields_cities_failed)
        LocationFieldsCityStatus.Short -> labels.typeMore?.invoke(config.minQuery)
            ?: pluralStringResource(R.plurals.monarch_locationfields_type_more, config.minQuery, config.minQuery)
        else -> monarchString(labels.noMatch, R.string.monarch_locationfields_no_match)
    }
    val useTyped = stringResource(R.string.monarch_locationfields_use_typed)
    val locale = LocalConfiguration.current.locales[0]
    val base = labels.combobox
    return MonarchComboboxLabels(
        search = base.search,
        loading = base.loading,
        empty = empty,
        create = labels.useTyped ?: base.create ?: { text -> String.format(locale, useTyped, text) },
        more = base.more,
        cancel = base.cancel,
        clear = base.clear,
    )
}
