package com.rookhq.monarch.components.geo.countryselect

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.forms.combobox.MonarchCombobox
import com.rookhq.monarch.components.forms.combobox.MonarchComboboxLabels
import com.rookhq.monarch.components.forms.combobox.MonarchComboboxOption
import com.rookhq.monarch.components.forms.combobox.MonarchComboboxPresentation
import com.rookhq.monarch.components.forms.combobox.MonarchComboboxTokens

/**
 * The MonarchCombobox under every geo picker. It keeps the search text, so the
 * "Suggested" heading is shown only while nothing is typed and the list is
 * never split into repeated headings by the match order.
 */
@Composable
@Suppress("LongParameterList")
internal fun CountrySelectField(
    options: List<MonarchComboboxOption>,
    selected: String?,
    onSelectedChange: (String?) -> Unit,
    priority: List<String>,
    texts: CountrySelectFieldTexts,
    modifier: Modifier,
    presentation: MonarchComboboxPresentation,
    enabled: Boolean,
    sortByMatch: Boolean,
    clearIcon: (@Composable () -> Unit)?,
    filter: ((MonarchComboboxOption, String) -> Boolean)? = CountrySelectData::matches,
    initiallyOpen: Boolean = false,
    required: Boolean = false,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val arranged = CountrySelectData.arrange(options, priority, query.isNotBlank(), texts.suggested, texts.all)
    MonarchCombobox(
        label = texts.label,
        options = arranged,
        selected = selected,
        onSelectedChange = onSelectedChange,
        modifier = modifier,
        presentation = MonarchComboboxTokens.presentation(presentation, options.size, searchesInApp = false),
        enabled = enabled,
        sortByMatch = sortByMatch,
        placeholder = texts.placeholder,
        helperText = texts.helperText,
        errorText = texts.errorText,
        clearOptionLabel = texts.clearOptionLabel,
        query = query,
        onQueryChange = { query = it },
        filter = filter,
        clearIcon = clearIcon,
        labels = texts.combobox,
        initiallyOpen = initiallyOpen,
        labelVisible = texts.labelVisible,
        required = required,
    )
}

/** [base] with its "no matches" text replaced by [empty]. */
internal fun countrySelectComboboxLabels(base: MonarchComboboxLabels, empty: String): MonarchComboboxLabels =
    MonarchComboboxLabels(
        search = base.search,
        loading = base.loading,
        empty = empty,
        create = base.create,
        more = base.more,
        cancel = base.cancel,
        clear = base.clear,
    )
