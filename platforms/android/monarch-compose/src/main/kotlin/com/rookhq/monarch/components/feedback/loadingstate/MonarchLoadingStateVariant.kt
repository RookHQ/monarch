package com.rookhq.monarch.components.feedback.loadingstate

/** The placeholder layout of a `MonarchLoadingState`. The same names exist on every platform. */
public enum class MonarchLoadingStateVariant {
    /** Rows of a round avatar and two lines of text. The default. */
    List,

    /** Rows of a line of text and a short value, for tables and settings lists. */
    Rows,

    /** Blocks, one under the other, for a list of cards. */
    Cards,

    /** A title and paragraphs, for a detail screen. */
    Detail,

    /** A centred progress ring with the label shown. */
    Spinner,
}
