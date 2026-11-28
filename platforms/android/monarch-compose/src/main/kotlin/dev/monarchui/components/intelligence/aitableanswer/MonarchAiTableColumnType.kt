package dev.monarchui.components.intelligence.aitableanswer

/** How the values of a `MonarchAiTableAnswer` column compare and format. */
public enum class MonarchAiTableColumnType {
    /** Text, sorted ignoring case and accents, with numbers inside the text in numeric order. */
    Text,

    /** Numbers: JSON numbers or text such as "1,200", "12%" and "−3". Sorted by value, formatted for the locale. */
    Number,

    /** ISO 8601 dates and times such as "2024-10-14" or "2024-10-14T09:30:00Z". Sorted by time, shown as given. */
    Date,
}
