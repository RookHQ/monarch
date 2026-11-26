package com.rookhq.monarch.components.geo.currencyselect

/** What a `MonarchCurrencySelect` shows for each currency as its label, in the list and in the field. */
public enum class MonarchCurrencySelectDisplay {
    /** The name: "Euro". */
    Name,

    /** The code, with the name as a second line in the list: "EUR". */
    Code,

    /** The code and the name, with the name as a second line in the list: "EUR · Euro". */
    CodeAndName,
}
