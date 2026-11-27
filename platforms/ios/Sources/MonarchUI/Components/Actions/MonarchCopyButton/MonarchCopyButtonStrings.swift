/// Built-in strings of the copy button, from the `MonarchCopyButton` strings table.
enum MonarchCopyButtonStrings {
    /// The title before a copy.
    static var copy: String { MonarchStrings.string("monarch.copyButton.copy", table: "MonarchCopyButton") }
    /// The title after a copy.
    static var copied: String { MonarchStrings.string("monarch.copyButton.copied", table: "MonarchCopyButton") }
    /// Announced by VoiceOver after a copy.
    static var announcement: String {
        MonarchStrings.string("monarch.copyButton.announcement", table: "MonarchCopyButton")
    }
}
