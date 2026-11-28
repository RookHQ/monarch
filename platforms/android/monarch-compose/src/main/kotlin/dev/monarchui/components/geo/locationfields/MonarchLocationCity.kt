package dev.monarchui.components.geo.locationfields

import androidx.compose.runtime.Immutable
import dev.monarchui.components.geo.regionselect.MonarchRegionSelectRegion

/**
 * A city from the app's data, as returned by `MonarchLocationAdapter.cities`.
 *
 * @property id the stable id to store, such as the data source's place id.
 * @property name the name people read.
 * @property description secondary text in the list, such as the region the city is in.
 * @property region the region the city belongs to. When a city is chosen before a region, this fills the region.
 */
@Immutable
public class MonarchLocationCity(
    public val id: String,
    public val name: String,
    public val description: String? = null,
    public val region: MonarchRegionSelectRegion? = null,
)
