package com.rookhq.monarch.components.geo.addressautocomplete

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.feedback.inlineerrorretry.MonarchInlineErrorRetry
import com.rookhq.monarch.components.forms.formfield.MonarchFieldFrame
import com.rookhq.monarch.components.forms.formfield.monarchFieldSemantics
import com.rookhq.monarch.components.forms.textfield.MonarchFieldInput
import com.rookhq.monarch.components.overlays.menu.MonarchMenuTokens
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.internal.monarchSurface
import com.rookhq.monarch.internal.MonarchLiveStatus
import com.rookhq.monarch.theme.MonarchTheme
import com.rookhq.monarch.tokens.MonarchTokens

/** The label, guidance and state of the inline search field. */
internal class AddressAutocompleteInlineFrame(
    val label: String,
    val helperText: String?,
    val errorText: String?,
    val enabled: Boolean,
    val required: Boolean,
)

/** The text, the open state and the callbacks of the inline search field. */
internal class AddressAutocompleteInlineState(
    val query: String,
    val onQueryChange: (String) -> Unit,
    val summary: String?,
    val placeholder: String,
    val open: Boolean,
    val onOpen: () -> Unit,
    val onDone: () -> Unit,
    val searchIcon: (@Composable () -> Unit)?,
)

/**
 * The web layout of the search: the label and an editable field with the search icon, the guidance under it, and
 * while it is open a card of suggestions (not a sheet) with the manual entry row at its end.
 */
@Composable
internal fun AddressAutocompleteInlineSearch(
    frame: AddressAutocompleteInlineFrame,
    state: AddressAutocompleteInlineState,
    searcher: AddressAutocompleteSearcher,
    texts: AddressAutocompleteSheetTexts,
    allowManualEntry: Boolean,
    onPick: (MonarchAddressAutocompleteSuggestion) -> Unit,
    onManual: () -> Unit,
    suggestionIcon: (@Composable () -> Unit)?,
) {
    val search = rememberAddressAutocompleteSearch(state.query, searcher)
    val invalid = !frame.errorText.isNullOrBlank()
    MonarchFieldFrame(
        label = frame.label,
        required = frame.required,
        enabled = frame.enabled,
        helperText = frame.helperText,
        errorText = frame.errorText,
    ) {
        MonarchFieldInput(
            value = if (state.open) state.query else state.summary.orEmpty(),
            onValueChange = state.onQueryChange,
            modifier = Modifier
                .onFocusChanged { if (it.isFocused && !state.open) state.onOpen() }
                .monarchFieldSemantics(frame.label, frame.errorText),
            enabled = frame.enabled,
            invalid = invalid,
            placeholder = state.placeholder,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { state.onDone() }),
            leading = state.searchIcon,
        )
    }
    if (state.open) {
        AddressAutocompleteCard(
            content = MonarchAddressAutocompleteTokens.content(state.query, searcher.minQueryLength, search.search),
            search = search,
            texts = texts,
            allowManualEntry = allowManualEntry,
            onPick = onPick,
            onManual = onManual,
            suggestionIcon = suggestionIcon,
        )
    }
}

/** The card of suggestions under the field: a status line for the cases without results, the rows, the manual row. */
@Composable
private fun AddressAutocompleteCard(
    content: AddressAutocompleteContent,
    search: AddressAutocompleteSearchState,
    texts: AddressAutocompleteSheetTexts,
    allowManualEntry: Boolean,
    onPick: (MonarchAddressAutocompleteSuggestion) -> Unit,
    onManual: () -> Unit,
    suggestionIcon: (@Composable () -> Unit)?,
) {
    val colors = MonarchTheme.colors
    val shape = MonarchTheme.shapes.lg
    val shadow = if (MonarchTheme.isDark) MonarchTokens.Space.space0.dp else MonarchMenuTokens.ShadowElevation
    val border = MonarchMenuTokens.border(colors, MonarchTheme.isDark, MonarchTheme.isHighContrast)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(shadow, shape)
            .monarchSurface(
                MonarchSurfaceColors(container = colors.bgRaised, content = colors.textDefault, border = border),
                shape,
            ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = MonarchAddressAutocompleteTokens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(MonarchAddressAutocompleteTokens.RowTextGap),
        ) {
            if (content == AddressAutocompleteContent.Failed) {
                MonarchInlineErrorRetry(message = texts.searchFailed, onRetry = search.retry)
            } else {
                AddressAutocompleteMessage(content, search.search.results.size, texts)
            }
            if (content == AddressAutocompleteContent.Suggestions) {
                search.search.results.forEach { suggestion ->
                    AddressAutocompleteRow(suggestion, suggestionIcon) { onPick(suggestion) }
                }
            }
            if (allowManualEntry) {
                Text(
                    text = texts.enterManually,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = MonarchAddressAutocompleteTokens.RowMinHeight)
                        .clickable(role = Role.Button, onClick = onManual)
                        .padding(vertical = MonarchAddressAutocompleteTokens.RowVerticalPadding),
                    style = MonarchTheme.typography.label,
                    color = colors.dangerStrong,
                )
            }
        }
    }
}

/** A message line that is always read politely: the number found stays silent on screen, as on the web. */
@Composable
private fun AddressAutocompleteMessage(
    content: AddressAutocompleteContent,
    count: Int,
    texts: AddressAutocompleteSheetTexts,
) {
    val message = when (content) {
        AddressAutocompleteContent.Searching -> texts.searching
        AddressAutocompleteContent.TooShort -> texts.tooShort
        AddressAutocompleteContent.Empty -> texts.empty
        AddressAutocompleteContent.Suggestions -> texts.results(count)
        else -> ""
    }
    val visible = content != AddressAutocompleteContent.Suggestions && message.isNotEmpty()
    MonarchLiveStatus(
        text = message.ifEmpty { null },
        visible = visible,
        modifier = if (visible) {
            Modifier.fillMaxWidth().padding(vertical = MonarchAddressAutocompleteTokens.RowVerticalPadding)
        } else {
            Modifier
        },
    )
}
