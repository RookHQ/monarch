package com.rookhq.monarch.components.geo.locationfields

import androidx.compose.runtime.Immutable

/**
 * What `MonarchLocationAdapter.cities` is asked for.
 *
 * @property country the chosen country code.
 * @property region the chosen region code, or `null` to search the whole country.
 * @property query what the person typed, trimmed. Empty when a chosen region's cities are listed.
 */
@Immutable
public data class MonarchLocationCityQuery(
    public val country: String,
    public val region: String?,
    public val query: String,
)
