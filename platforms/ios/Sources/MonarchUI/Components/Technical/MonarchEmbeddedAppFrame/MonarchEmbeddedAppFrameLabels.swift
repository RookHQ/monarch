/// Replacements for the built-in strings of ``MonarchEmbeddedAppFrame``. Each `nil` keeps the built-in text.
public struct MonarchEmbeddedAppFrameLabels: Sendable, Hashable {
    /// The loading text. Built-in: "Loading…".
    public var loading: String?
    /// The failure title. Built-in: "This page didn’t open".
    public var errorTitle: String?
    /// The failure message.
    public var errorDescription: String?
    /// The retry button. Built-in: "Try again".
    public var retry: String?
    /// The button that opens the page outside the frame. Built-in: "Open page".
    public var open: String?

    /// Replacement strings.
    ///
    /// - Parameters:
    ///   - loading: The loading text.
    ///   - errorTitle: The failure title.
    ///   - errorDescription: The failure message.
    ///   - retry: The retry button.
    ///   - open: The button that opens the page outside the frame, such as "Open in Example Desk".
    public init(
        loading: String? = nil,
        errorTitle: String? = nil,
        errorDescription: String? = nil,
        retry: String? = nil,
        open: String? = nil
    ) {
        self.loading = loading
        self.errorTitle = errorTitle
        self.errorDescription = errorDescription
        self.retry = retry
        self.open = open
    }
}
