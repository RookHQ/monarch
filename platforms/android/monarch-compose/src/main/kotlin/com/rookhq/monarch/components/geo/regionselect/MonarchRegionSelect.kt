package com.rookhq.monarch.components.geo.regionselect

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.rookhq.monarch.R
import com.rookhq.monarch.components.feedback.inlineerrorretry.MonarchInlineErrorRetry
import com.rookhq.monarch.components.forms.combobox.MonarchCombobox
import com.rookhq.monarch.components.forms.combobox.MonarchComboboxLabels
import com.rookhq.monarch.components.forms.combobox.MonarchComboboxOption
import com.rookhq.monarch.components.forms.combobox.MonarchComboboxPresentation
import com.rookhq.monarch.components.forms.textfield.MonarchTextField
import com.rookhq.monarch.components.geo.countryselect.countrySelectComboboxLabels
import com.rookhq.monarch.internal.monarchString
import kotlinx.coroutines.CancellationException
import java.util.Locale

/**
 * Chooses a first-level subdivision of a country: a state, province,
 * territory or nation.
 *
 * Built on `MonarchCombobox`, so it is the Material 3 exposed dropdown field
 * with a filtered menu or a bottom sheet with a search field. The regions come
 * from [regions], else from [loadRegions] (once per country, kept for the life
 * of the field, with a retry when it fails), else from the built-in lists of
 * the United States, Canada, Australia, the United Kingdom and India. A
 * country with no list gets a `MonarchTextField` instead, filled by autofill
 * as an address region. Without a [country] the field is disabled and says
 * "Choose a country first". The label is the country's own word for its
 * regions ("State", "Province or territory") unless [labels] replaces it.
 *
 * @param country the ISO 3166-1 alpha-2 code of the country whose regions are listed, or `null`.
 * @param selected the chosen region code, or the typed text when the country has no list, or `null`.
 * @param onSelectedChange called with the chosen region code or the typed text, or `null` when cleared or emptied.
 * @param modifier the modifier of the field.
 * @param regions the country's regions from the app's own data. Wins over [loadRegions] and the built-in lists.
 * @param loadRegions loads a country's regions, for example from the app's API. Called once per country; an empty
 * list gives the text field. Wins over the built-in lists, so call your own fallback inside it to keep them.
 * @param presentation a menu, a sheet, or automatic: a menu for up to eight regions, a sheet otherwise.
 * @param enabled whether the field can be used.
 * @param allowTextEntry shows a text field when the country has no list. Otherwise the field stays a combobox with no
 * options.
 * @param showCode shows the region code without its country prefix ("CA" for "US-CA") at the end of each row.
 * @param placeholder shown in the empty field while nothing is chosen.
 * @param helperText guidance below the field.
 * @param errorText the error message; the field is in the error state.
 * @param clearOptionLabel adds a first row that clears the selection.
 * @param clearIcon the icon of a clear button at the end of the field while a region is chosen.
 * @param labels replacements for the built-in strings.
 */
@Composable
@Suppress("LongParameterList")
public fun MonarchRegionSelect(
    country: String?,
    selected: String?,
    onSelectedChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
    regions: List<MonarchRegionSelectRegion>? = null,
    loadRegions: (suspend (country: String) -> List<MonarchRegionSelectRegion>)? = null,
    presentation: MonarchComboboxPresentation = MonarchComboboxPresentation.Automatic,
    enabled: Boolean = true,
    allowTextEntry: Boolean = true,
    showCode: Boolean = false,
    placeholder: String? = null,
    helperText: String? = null,
    errorText: String? = null,
    clearOptionLabel: String? = null,
    clearIcon: (@Composable () -> Unit)? = null,
    labels: MonarchRegionSelectLabels = MonarchRegionSelectLabels(),
) {
    val code = country?.trim()?.uppercase(Locale.ROOT)?.ifEmpty { null }
    val source = regionSelectSource(code, regions, loadRegions)
    val label = monarchString(labels.label, RegionSelectData.labelRes(code))
    val combobox = countrySelectComboboxLabels(
        labels.combobox,
        monarchString(labels.empty, R.string.monarch_regionselect_empty),
    )
    val texts = RegionSelectTexts(label, placeholder, helperText, errorText, clearOptionLabel, combobox)
    when {
        source is RegionSelectSource.NoCountry -> RegionSelectCombobox(
            options = emptyList(),
            selected = null,
            onSelectedChange = onSelectedChange,
            modifier = modifier,
            texts = texts.copy(
                helperText = helperText ?: monarchString(
                    labels.chooseCountryFirst,
                    R.string.monarch_regionselect_choose_country_first,
                ),
            ),
            presentation = presentation,
            enabled = false,
        )
        source is RegionSelectSource.Ready && source.regions.isEmpty() && allowTextEntry -> MonarchTextField(
            value = selected.orEmpty(),
            onValueChange = { onSelectedChange(it.ifEmpty { null }) },
            modifier = modifier.semantics { contentType = ContentType.AddressRegion },
            label = label,
            placeholder = placeholder ?: monarchString(labels.textEntry, R.string.monarch_regionselect_text_entry),
            helperText = helperText,
            errorText = errorText,
            enabled = enabled,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
            ),
        )
        else -> key(code) {
            val list = (source as? RegionSelectSource.Ready)?.regions.orEmpty()
            Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MonarchRegionSelectTokens.Gap)) {
                RegionSelectCombobox(
                    options = remember(list, showCode) { list.map { RegionSelectData.option(it, showCode) } },
                    selected = selected,
                    onSelectedChange = onSelectedChange,
                    modifier = Modifier.fillMaxWidth(),
                    texts = texts,
                    presentation = presentation,
                    enabled = enabled,
                    loading = source is RegionSelectSource.Loading,
                    clearIcon = clearIcon,
                )
                if (source is RegionSelectSource.Failed) {
                    MonarchInlineErrorRetry(
                        message = monarchString(labels.loadFailed, R.string.monarch_regionselect_load_failed),
                        onRetry = { source.retry() },
                    )
                }
            }
        }
    }
}

/** The texts of the region field. */
private data class RegionSelectTexts(
    val label: String,
    val placeholder: String?,
    val helperText: String?,
    val errorText: String?,
    val clearOptionLabel: String?,
    val combobox: MonarchComboboxLabels,
)

/**
 * The regions for [code]: none without a country, the app's [regions], the built-in list without a loader, or the
 * loader's answer.
 */
@Composable
private fun regionSelectSource(
    code: String?,
    regions: List<MonarchRegionSelectRegion>?,
    loadRegions: (suspend (country: String) -> List<MonarchRegionSelectRegion>)?,
): RegionSelectSource {
    val usesLoader = code != null && regions == null && loadRegions != null
    val loaded = regionSelectLoaded(code.takeIf { usesLoader }, loadRegions)
    return when {
        code == null -> RegionSelectSource.NoCountry
        regions != null -> RegionSelectSource.Ready(regions)
        !usesLoader -> RegionSelectSource.Ready(RegionSelectData.builtIn(code).orEmpty())
        else -> loaded
    }
}

/**
 * Runs [loadRegions] once per country and keeps each answer, with a retry when it fails. Loading while [code] is
 * `null`.
 */
@Composable
private fun regionSelectLoaded(
    code: String?,
    loadRegions: (suspend (country: String) -> List<MonarchRegionSelectRegion>)?,
): RegionSelectSource {
    val cache = remember { mutableStateMapOf<String, List<MonarchRegionSelectRegion>>() }
    var failedFor by remember { mutableStateOf<String?>(null) }
    var attempt by remember { mutableIntStateOf(0) }
    val loader by rememberUpdatedState(loadRegions)
    LaunchedEffect(code, attempt) {
        val load = loader
        if (code == null || load == null || cache.containsKey(code)) return@LaunchedEffect
        failedFor = null
        try {
            cache[code] = load(code)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") failed: Exception) {
            // A failed load is shown with a retry, not thrown.
            failedFor = code
        }
    }
    val answer = code?.let { cache[it] }
    return when {
        answer != null -> RegionSelectSource.Ready(answer)
        code != null && failedFor == code -> RegionSelectSource.Failed { attempt++ }
        else -> RegionSelectSource.Loading
    }
}

/** The combobox of the region field. */
@Composable
private fun RegionSelectCombobox(
    options: List<MonarchComboboxOption>,
    selected: String?,
    onSelectedChange: (String?) -> Unit,
    modifier: Modifier,
    texts: RegionSelectTexts,
    presentation: MonarchComboboxPresentation,
    enabled: Boolean,
    loading: Boolean = false,
    clearIcon: (@Composable () -> Unit)? = null,
) {
    MonarchCombobox(
        label = texts.label,
        options = options,
        selected = selected,
        onSelectedChange = onSelectedChange,
        modifier = modifier,
        presentation = presentation,
        enabled = enabled,
        loading = loading,
        sortByMatch = true,
        placeholder = texts.placeholder,
        helperText = texts.helperText,
        errorText = texts.errorText,
        clearOptionLabel = texts.clearOptionLabel,
        clearIcon = clearIcon,
        labels = texts.combobox,
    )
}
