package dev.monarchui.components.intelligence.aicontextchips

/** What kind of thing an item of AI context is. TalkBack reads the kind before the name. */
public enum class MonarchAiContextKind {
    /** A file, such as a PDF or a spreadsheet. */
    File,

    /** Selected text or cells. */
    Selection,

    /** A page or a screen of the app. */
    Page,

    /** A record, such as a ticket, a deal or a row. */
    Record,

    /** A connected app or an integration. */
    App,

    /** A person. */
    Person,
}
