package dev.monarchui.components.geo.locationfields

/** One part of the address in `MonarchLocationFields`. */
public enum class MonarchLocationField {
    /** The country, an ISO 3166-1 alpha-2 code. */
    Country,

    /** The first-level subdivision: a state, province or region. */
    Region,

    /** The city or town. */
    City,

    /** The postal code. */
    PostalCode,
}
