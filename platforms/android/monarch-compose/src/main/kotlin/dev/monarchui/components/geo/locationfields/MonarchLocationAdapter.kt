package dev.monarchui.components.geo.locationfields

import androidx.compose.runtime.Immutable
import dev.monarchui.components.geo.regionselect.MonarchRegionSelectRegion

/**
 * Where `MonarchLocationFields` gets its lists. Monarch makes no network
 * calls: the app implements the functions against its own data or API. A
 * newer request cancels the coroutine of the older one; throw to show that
 * the list could not be loaded.
 *
 * @property countries the country codes to offer, such as the countries the product ships to. `null` offers every
 * country, named in the locale.
 * @property regions loads a country's regions. An empty list gives a text field. `null` uses Monarch's built-in lists
 * (United States, Canada, Australia, United Kingdom, India) and a text field for other countries.
 * @property regionLabel the country's own word for its regions, such as "Emirate". `null`, or `null` from it, keeps
 * the built-in word ("State", "Province or territory") or the label override.
 * @property cities searches a country's cities as the person types, or lists a chosen region's cities. `null` gives a
 * text field for the city.
 */
@Immutable
public class MonarchLocationAdapter(
    public val countries: List<String>? = null,
    public val regions: (suspend (country: String) -> List<MonarchRegionSelectRegion>)? = null,
    public val regionLabel: ((country: String) -> String?)? = null,
    public val cities: (suspend (query: MonarchLocationCityQuery) -> List<MonarchLocationCity>)? = null,
)
