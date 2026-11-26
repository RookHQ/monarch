/// Built-in strings of the embedded app frame, from the `MonarchEmbeddedAppFrame` strings table.
enum MonarchEmbeddedAppFrameStrings {
    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.embeddedAppFrame.\(key)", table: "MonarchEmbeddedAppFrame")
    }

    /// The loading text.
    static var loading: String { string("loading") }
    /// The failure title.
    static var errorTitle: String { string("errorTitle") }
    /// The failure message.
    static var errorDescription: String { string("errorDescription") }
    /// The retry button.
    static var retry: String { string("retry") }
    /// The button that opens the page outside the frame.
    static var open: String { string("open") }
}
