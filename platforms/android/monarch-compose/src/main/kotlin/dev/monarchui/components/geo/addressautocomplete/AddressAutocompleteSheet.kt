package dev.monarchui.components.geo.addressautocomplete

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.feedback.inlineerrorretry.MonarchInlineErrorRetry
import dev.monarchui.components.overlays.bottomsheet.MonarchBottomSheet
import dev.monarchui.components.search.searchbar.MonarchSearchBar
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/** The texts of the suggestion sheet. */
internal class AddressAutocompleteSheetTexts(
    val title: String,
    val placeholder: String,
    val searching: String,
    val empty: String,
    val searchFailed: String,
    val enterManually: String,
    val cancel: String,
    val tooShort: String,
    val results: (Int) -> String,
)

/** How the sheet searches: the app's provider, the minimum length and the debounce. */
internal class AddressAutocompleteSearcher(
    val search: suspend (String) -> List<MonarchAddressAutocompleteSuggestion>,
    val minQueryLength: Int,
    val debounceMillis: Long,
)

/**
 * The suggestion list of MonarchAddressAutocomplete: a Material 3 modal
 * bottom sheet with the label as its title, a `MonarchSearchBar`, a status
 * line that TalkBack reads politely, the suggestions, the manual entry row and
 * Cancel. The search runs after the debounce once the text is long enough; a
 * newer text cancels the older search, so a late answer is dropped.
 */
@Composable
@Suppress("LongParameterList")
internal fun AddressAutocompleteSheet(
    query: String,
    onQueryChange: (String) -> Unit,
    searcher: AddressAutocompleteSearcher,
    texts: AddressAutocompleteSheetTexts,
    allowManualEntry: Boolean,
    onPick: (MonarchAddressAutocompleteSuggestion) -> Unit,
    onManual: () -> Unit,
    onCancel: () -> Unit,
    suggestionIcon: (@Composable () -> Unit)?,
) {
    val state = rememberAddressAutocompleteSearch(query, searcher)
    val search = state.search
    val content = MonarchAddressAutocompleteTokens.content(query, searcher.minQueryLength, search)
    MonarchBottomSheet(onDismissRequest = onCancel, title = texts.title) {
        Column(verticalArrangement = Arrangement.spacedBy(MonarchAddressAutocompleteTokens.SheetGap)) {
            MonarchSearchBar(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = {},
                modifier = Modifier.fillMaxWidth(),
                pending = content == AddressAutocompleteContent.Searching,
                placeholder = texts.placeholder,
            )
            AddressAutocompleteStatus(content, search.results.size, texts, state.retry)
            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                if (content == AddressAutocompleteContent.Suggestions) {
                    items(search.results, key = { it.id }) { suggestion ->
                        AddressAutocompleteRow(suggestion, suggestionIcon) { onPick(suggestion) }
                    }
                }
                if (allowManualEntry) {
                    item(key = "manual") {
                        MonarchButton(
                            text = texts.enterManually,
                            onClick = onManual,
                            variant = MonarchButtonVariant.Ghost,
                        )
                    }
                }
            }
            MonarchButton(
                text = texts.cancel,
                onClick = onCancel,
                modifier = Modifier.align(Alignment.End),
                variant = MonarchButtonVariant.Ghost,
            )
        }
    }
}

/**
 * The status of the list: the retry message when the search failed, else a
 * line that is always present and read politely by TalkBack when it changes:
 * searching, too short, nothing found, or the number found.
 */
@Composable
internal fun AddressAutocompleteStatus(
    content: AddressAutocompleteContent,
    count: Int,
    texts: AddressAutocompleteSheetTexts,
    onRetry: () -> Unit,
) {
    if (content == AddressAutocompleteContent.Failed) {
        MonarchInlineErrorRetry(message = texts.searchFailed, onRetry = onRetry)
        return
    }
    val message = when (content) {
        AddressAutocompleteContent.Searching -> texts.searching
        AddressAutocompleteContent.TooShort -> texts.tooShort
        AddressAutocompleteContent.Empty -> texts.empty
        AddressAutocompleteContent.Suggestions -> texts.results(count)
        else -> ""
    }
    Text(
        text = message,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        style = MonarchTheme.typography.caption,
        color = MonarchAddressAutocompleteTokens.mutedColor(MonarchTheme.colors),
    )
}

/** One suggestion: one button, read as its label and description, at least 48dp high. The icon is decorative. */
@Composable
internal fun AddressAutocompleteRow(
    suggestion: MonarchAddressAutocompleteSuggestion,
    icon: (@Composable () -> Unit)?,
    onClick: () -> Unit,
) {
    val colors = MonarchTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MonarchAddressAutocompleteTokens.RowMinHeight)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = MonarchAddressAutocompleteTokens.RowVerticalPadding),
        horizontalArrangement = Arrangement.spacedBy(MonarchAddressAutocompleteTokens.RowGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            CompositionLocalProvider(LocalContentColor provides MonarchAddressAutocompleteTokens.mutedColor(colors)) {
                icon()
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(MonarchAddressAutocompleteTokens.RowTextGap),
        ) {
            Text(
                suggestion.label,
                style = MonarchTheme.typography.body,
                color = MonarchAddressAutocompleteTokens.labelColor(colors),
            )
            suggestion.description?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    style = MonarchTheme.typography.caption,
                    color = MonarchAddressAutocompleteTokens.mutedColor(colors),
                )
            }
        }
    }
}

/** The search of one text and the way to run it again. */
internal class AddressAutocompleteSearchState(val search: AddressAutocompleteSearch, val retry: () -> Unit)

/**
 * Runs the search for [query] after the debounce once it is long enough; a newer text cancels the older search, so a
 * late answer is dropped, and a failed search is kept as the failed state.
 */
@Composable
internal fun rememberAddressAutocompleteSearch(
    query: String,
    searcher: AddressAutocompleteSearcher,
): AddressAutocompleteSearchState {
    var search by remember { mutableStateOf(AddressAutocompleteSearch()) }
    var attempt by remember { mutableIntStateOf(0) }
    val provider by rememberUpdatedState(searcher.search)
    val trimmed = query.trim()
    LaunchedEffect(trimmed, attempt) {
        if (trimmed.length < searcher.minQueryLength) return@LaunchedEffect
        search = AddressAutocompleteSearch(query = trimmed, running = true)
        delay(searcher.debounceMillis)
        search = try {
            AddressAutocompleteSearch(query = trimmed, results = provider(trimmed))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") failed: Exception) {
            // A failed search is shown with Try again, not thrown.
            AddressAutocompleteSearch(query = trimmed, failed = true)
        }
    }
    return AddressAutocompleteSearchState(search) { attempt++ }
}
