package com.rookhq.monarch.components.geo.locationfields

/** What the city field needs from MonarchLocationFields. */
internal class LocationFieldsCityConfig(
    val label: String,
    val errorText: String?,
    val enabled: Boolean,
    val minQuery: Int,
    val debounceMillis: Long,
    val allowCustom: Boolean,
    val labels: MonarchLocationFieldsLabels,
    val required: Boolean = false,
)
