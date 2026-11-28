package dev.monarchui.components.geo.worldmap

/** How a `MonarchWorldMap` shows its values. */
public enum class MonarchWorldMapVariant {
    /** Each country is filled with the step of its value, with a Less to More legend. */
    Choropleth,

    /** A circle whose area grows with the value sits on each country, over a neutral map. */
    Bubble,
}
