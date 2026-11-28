package dev.monarchui.components.navigation.tabs

/** How the tabs of a `MonarchTabs` are drawn. */
public enum class MonarchTabsVariant {
    /** Labels with an accent underline under the selected tab and a divider under the row. The default. */
    Line,

    /** A muted track with the selected tab raised on it, for a short choice of views such as Day, Week, Month. */
    Pill,
}
