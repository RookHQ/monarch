package dev.monarchui.components.geo.addressautocomplete

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.accessibility.MonarchTouchTarget
import dev.monarchui.components.geo.countryselect.CountrySelectData
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens
import java.util.Locale

/** What the suggestion list of a MonarchAddressAutocomplete shows. */
internal enum class AddressAutocompleteContent {
    /** Nothing is typed. */
    Idle,

    /** The text is shorter than the minimum. */
    TooShort,

    /** Waiting for the debounce or the provider. */
    Searching,

    /** The provider failed. */
    Failed,

    /** The provider found nothing. */
    Empty,

    /** The provider's suggestions. */
    Suggestions,
}

/** The state of the search for one text: whether it runs or failed, and what it found. */
internal data class AddressAutocompleteSearch(
    val query: String = "",
    val running: Boolean = false,
    val failed: Boolean = false,
    val results: List<MonarchAddressAutocompleteSuggestion> = emptyList(),
)

/** Component tokens and the pure rules of MonarchAddressAutocomplete. */
internal object MonarchAddressAutocompleteTokens {
    /** The gap between the field, the progress, the form and the buttons. */
    val Gap: Dp = MonarchTokens.Space.space3.dp

    /** The gap between the parts of the sheet. */
    val SheetGap: Dp = MonarchTokens.Space.space3.dp

    /** The least height of a suggestion row: the touch target. */
    val RowMinHeight: Dp = MonarchTouchTarget.Minimum

    /** The vertical padding of a suggestion row, and the gap between its icon and its text. */
    val RowVerticalPadding: Dp = MonarchTokens.Space.space2.dp
    val RowGap: Dp = MonarchTokens.Space.space3.dp

    /** The padding at the sides of the inline suggestion card. */
    val CardPadding: Dp = MonarchTokens.Space.space3.dp

    /** The gap between a suggestion's label and its description. */
    val RowTextGap: Dp = MonarchTokens.Space.space0_5.dp

    /** The mode to start in: the form when the address already has a street line, else the search field. */
    fun initialMode(address: MonarchAddressAutocompleteAddress): MonarchAddressAutocompleteMode =
        if (address.line1.isNotBlank()) MonarchAddressAutocompleteMode.Manual else MonarchAddressAutocompleteMode.Search

    /** The address on one line, with the country named in [locale], or `null` when it is empty. */
    fun summary(address: MonarchAddressAutocompleteAddress, locale: Locale): String? {
        val country = address.countryCode?.trim()?.takeIf { it.isNotEmpty() }?.let {
            CountrySelectData.name(CountrySelectData.canonical(it), locale)
        }
        return listOfNotNull(address.line1, address.line2, address.city, address.region, address.postalCode, country)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(", ")
            .ifEmpty { null }
    }

    /** The street line kept when leaving the search for the form: the current one, else the typed text. */
    fun manualLine1(current: String, query: String): String = current.ifBlank { query.trim() }

    /** What the list shows for the trimmed [query] and the [search] state. */
    fun content(query: String, minQueryLength: Int, search: AddressAutocompleteSearch): AddressAutocompleteContent {
        val trimmed = query.trim()
        return when {
            trimmed.isEmpty() -> AddressAutocompleteContent.Idle
            trimmed.length < minQueryLength -> AddressAutocompleteContent.TooShort
            search.query != trimmed || search.running -> AddressAutocompleteContent.Searching
            search.failed -> AddressAutocompleteContent.Failed
            search.results.isEmpty() -> AddressAutocompleteContent.Empty
            else -> AddressAutocompleteContent.Suggestions
        }
    }

    /** The colour of a suggestion's label. */
    fun labelColor(colors: MonarchColors): Color = colors.textDefault

    /** The colour of a suggestion's description, of the list messages and of the progress text. */
    fun mutedColor(colors: MonarchColors): Color = colors.textMuted
}
