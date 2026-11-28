package dev.monarchui.components.geo.locationfields

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.components.forms.combobox.MonarchComboboxOption
import dev.monarchui.components.geo.regionselect.MonarchRegionSelectRegion
import dev.monarchui.tokens.MonarchTokens

/** Where the city search is. */
internal enum class LocationFieldsCityStatus {
    /** Showing the results, or nothing to search yet. */
    Idle,

    /** A search runs. */
    Loading,

    /** The last search failed. */
    Failed,

    /** Fewer letters than the minimum were typed and no region is chosen. */
    Short,
}

/** Component tokens and the pure logic of MonarchLocationFields. */
internal object MonarchLocationFieldsTokens {
    /** The gap between the fields. */
    val Gap: Dp = MonarchTokens.Space.space4.dp

    /** The prefix of the option id of a typed city, which no data source uses. */
    const val TYPED_PREFIX: String = "\u0000typed:"

    /** The default number of letters before cities are searched without a region. */
    const val DEFAULT_CITY_MIN_QUERY: Int = 2

    /** The default wait after the last keystroke before cities are searched. */
    const val DEFAULT_CITY_DEBOUNCE_MILLIS: Long = 250L

    /** A new country: every other part is cleared. `null` when the country did not change. */
    fun countryChanged(value: MonarchLocationValue, code: String?): MonarchLocationValue? =
        if (code == value.country) null else MonarchLocationValue(country = code)

    /**
     * A region from the region field: a code from [list], or typed text. Either way the city is cleared when the
     * region changes.
     */
    fun regionChanged(
        value: MonarchLocationValue,
        selected: String?,
        list: List<MonarchRegionSelectRegion>?,
    ): MonarchLocationValue {
        val chosen = list?.firstOrNull { it.code == selected }
        val next = if (chosen != null) {
            value.copy(region = chosen.code, regionName = chosen.name)
        } else {
            value.copy(region = null, regionName = selected.orEmpty())
        }
        val same = next.region == value.region && next.regionName == value.regionName
        return if (same) next else next.copy(city = "", cityId = null)
    }

    /** The region field's selection: the region code, else the typed name, else `null`. */
    fun regionSelection(value: MonarchLocationValue): String? = value.region ?: value.regionName.ifEmpty { null }

    /** The city combobox's selection: the city id, the typed city as an option id, or `null`. */
    fun citySelection(value: MonarchLocationValue): String? =
        value.cityId ?: value.city.takeIf { it.isNotEmpty() }?.let { TYPED_PREFIX + it }

    /** The options of the city combobox: the results, and the chosen city when it is not among them. */
    fun cityOptions(results: List<MonarchLocationCity>, value: MonarchLocationValue): List<MonarchComboboxOption> {
        val options = results.map { MonarchComboboxOption(id = it.id, label = it.name, description = it.description) }
        val selection = citySelection(value) ?: return options
        return if (options.any { it.id == selection }) {
            options
        } else {
            options + MonarchComboboxOption(id = selection, label = value.city)
        }
    }

    /** The option for a typed city, offered when it is not in the list. `null` for blank text. */
    fun typedOption(text: String): MonarchComboboxOption? =
        text.trim().takeIf { it.isNotEmpty() }?.let { MonarchComboboxOption(id = TYPED_PREFIX + it, label = it) }

    /**
     * A city from the combobox: cleared, typed, or chosen from [results], which also fills an empty region. `null`
     * when the id is unknown.
     */
    fun cityChosen(
        value: MonarchLocationValue,
        id: String?,
        results: List<MonarchLocationCity>,
    ): MonarchLocationValue? {
        val city = results.firstOrNull { it.id == id }
        val region = city?.region
        return when {
            id == null -> value.copy(city = "", cityId = null)
            id.startsWith(TYPED_PREFIX) -> value.copy(city = id.removePrefix(TYPED_PREFIX), cityId = null)
            id == value.cityId -> value
            city == null -> null
            region != null && value.region == null ->
                value.copy(city = city.name, cityId = city.id, region = region.code, regionName = region.name)
            else -> value.copy(city = city.name, cityId = city.id)
        }
    }

    /** Whether a city search runs for this text, or the list asks for more letters. */
    fun needsMoreLetters(query: String, region: String?, minQuery: Int): Boolean =
        region == null && query.trim().length < minQuery
}
