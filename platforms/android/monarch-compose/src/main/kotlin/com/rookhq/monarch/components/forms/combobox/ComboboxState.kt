package com.rookhq.monarch.components.forms.combobox

import androidx.compose.runtime.Composable

/** What a combobox field and its list show, resolved once per composition. */
internal class ComboboxState(
    val label: String,
    val chosen: MonarchComboboxOption?,
    val selected: String?,
    val query: String,
    val expanded: Boolean,
    val enabled: Boolean,
    val loading: Boolean,
    val entries: List<ComboboxEntry>,
    val texts: ComboboxTexts,
    val labelVisible: Boolean = true,
    val required: Boolean = false,
)

/** The texts of a combobox field and its sheet. */
internal class ComboboxTexts(
    val placeholder: String?,
    val helperText: String?,
    val errorText: String?,
    val search: String,
    val clearName: String,
    val cancel: String,
)

/** What a combobox field and its list can do. */
internal class ComboboxActions(
    val open: () -> Unit,
    val close: () -> Unit,
    val type: (String) -> Unit,
    val choose: (ComboboxEntry) -> Unit,
    val clear: (() -> Unit)?,
    val clearIcon: (@Composable () -> Unit)?,
)
