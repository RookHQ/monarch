/// What the body of a ``MonarchWidget`` shows.
public enum MonarchWidgetState: Sendable, Hashable {
    /// The content.
    case ready
    /// A placeholder of three lines, read as "Loading" and the title.
    case loading
    /// Nothing to show: a small empty state with this title, or the
    /// built-in "Nothing to show yet".
    case empty(String? = nil)
    /// The data did not load: this message, or the built-in "This section
    /// could not be loaded.", with Try again when the widget has `onRetry`.
    case failed(String? = nil)
}
