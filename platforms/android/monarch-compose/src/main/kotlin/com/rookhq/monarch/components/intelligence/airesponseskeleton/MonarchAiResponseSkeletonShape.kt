package com.rookhq.monarch.components.intelligence.airesponseskeleton

/** The kind of answer a `MonarchAiResponseSkeleton` stands in for. The same names exist on every platform. */
public enum class MonarchAiResponseSkeletonShape {
    /** Paragraph lines with a shorter last line. The default. */
    Text,

    /** A column of card outlines, each with a title line and two lines of text. */
    Cards,

    /** A header row and body rows of cells. */
    Table,
}
