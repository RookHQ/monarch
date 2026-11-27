package com.rookhq.monarch.theme

/** Whether the theme uses the high-contrast palette. One of the three appearance inputs of [MonarchTheme]. */
public enum class MonarchContrast {
    /** Follow the system contrast setting (Android 14 and later; standard on earlier versions). */
    System,

    /** Always the standard palette. */
    Standard,

    /** Always the high-contrast palette. */
    More,
}
