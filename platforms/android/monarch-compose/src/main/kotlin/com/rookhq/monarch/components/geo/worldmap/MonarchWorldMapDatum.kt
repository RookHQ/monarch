package com.rookhq.monarch.components.geo.worldmap

import androidx.compose.runtime.Immutable

/**
 * The value of one country in a `MonarchWorldMap`.
 *
 * @property country the ISO 3166-1 alpha-2 code, such as "DE". Case and surrounding spaces do not matter; the
 * map trims and upper-cases it, and passes the cleaned code back in `onSelect`.
 * @property value the value. Values that are not finite are left out.
 * @property details extra lines for the inspected country and the table, such as users or sessions.
 */
@Immutable
public class MonarchWorldMapDatum(
    public val country: String,
    public val value: Double,
    public val details: List<MonarchWorldMapDetail> = emptyList(),
)
