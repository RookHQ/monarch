package dev.monarchui.components.geo.locationfields

import androidx.compose.runtime.Immutable

/**
 * The address held by `MonarchLocationFields`. The app keeps it and passes it
 * back; changing the country clears every other part, and changing the
 * region clears the city.
 *
 * @property country the country code (ISO 3166-1 alpha-2), or `null`.
 * @property region the code of the region chosen from a list, such as "US-CA", or `null` when the region was typed
 * or not chosen.
 * @property regionName the region as people read it: the chosen region's name or the typed text.
 * @property city the city as people read it: the chosen city's name or the typed text.
 * @property cityId the id of the city chosen from the adapter's list, or `null` when the city was typed.
 * @property postalCode the postal code as typed.
 */
@Immutable
public data class MonarchLocationValue(
    public val country: String? = null,
    public val region: String? = null,
    public val regionName: String = "",
    public val city: String = "",
    public val cityId: String? = null,
    public val postalCode: String = "",
)
