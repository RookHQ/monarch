package com.rookhq.monarch.components.datadisplay.descriptionlist

/** Where the term of each item of a `MonarchDescriptionList` goes. */
public enum class MonarchDescriptionListLayout {
    /** The term on its own line above the value. */
    Stacked,

    /** The term at the start and the value beside it; stacked again when the row is narrow or the text large. */
    Inline,
}
