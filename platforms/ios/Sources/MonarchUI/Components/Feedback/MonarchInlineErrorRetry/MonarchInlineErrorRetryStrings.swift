/// Built-in strings of the inline error row, from the
/// `MonarchInlineErrorRetry` strings table.
enum MonarchInlineErrorRetryStrings {
    private static let table = "MonarchInlineErrorRetry"

    /// The title of the retry button.
    static var retry: String { MonarchStrings.string("monarch.inlineErrorRetry.retry", table: table) }
    /// Announced when a retry starts, and the value of the button while it runs.
    static var retrying: String { MonarchStrings.string("monarch.inlineErrorRetry.retrying", table: table) }
}
