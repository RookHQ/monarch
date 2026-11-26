/// Replacements for the built-in strings of ``MonarchReceiptPreview``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchReceiptPreviewLabels: Sendable, Hashable {
    /// The button title. Built in: "View receipt".
    public var view: String?
    /// Shown instead of the button when there is no receipt. Built in: "No receipt".
    public var none: String?
    /// Spoken while the file loads. Built in: "Loading receipt".
    public var loading: String?
    /// Shown when loading fails. Built in: "The receipt could not be loaded."
    public var loadError: String?
    /// The retry button title. Built in: the inline error retry's "Try again".
    public var retry: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        view: String? = nil,
        none: String? = nil,
        loading: String? = nil,
        loadError: String? = nil,
        retry: String? = nil
    ) {
        self.view = view
        self.none = none
        self.loading = loading
        self.loadError = loadError
        self.retry = retry
    }
}
