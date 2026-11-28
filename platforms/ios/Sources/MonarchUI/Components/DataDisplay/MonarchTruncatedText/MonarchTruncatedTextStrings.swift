/// Built-in strings of the truncated text, from the `MonarchTruncatedText`
/// strings table.
enum MonarchTruncatedTextStrings {
    /// The title of the button that shows the whole text.
    static var showMore: String {
        MonarchStrings.string("monarch.truncatedText.showMore", table: "MonarchTruncatedText")
    }

    /// The title of the button that cuts the text again.
    static var showLess: String {
        MonarchStrings.string("monarch.truncatedText.showLess", table: "MonarchTruncatedText")
    }
}
