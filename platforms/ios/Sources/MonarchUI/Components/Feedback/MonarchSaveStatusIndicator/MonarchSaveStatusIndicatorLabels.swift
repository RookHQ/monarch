/// Replacements for the built-in strings of ``MonarchSaveStatusIndicator``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchSaveStatusIndicatorLabels: Sendable {
    /// Built in: "Unsaved changes".
    public var unsaved: String?
    /// Built in: "Saving…".
    public var saving: String?
    /// Built in: "Saved".
    public var saved: String?
    /// Built in: "Couldn't save".
    public var error: String?
    /// The retry button title. Built in: "Retry".
    public var retry: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        unsaved: String? = nil,
        saving: String? = nil,
        saved: String? = nil,
        error: String? = nil,
        retry: String? = nil
    ) {
        self.unsaved = unsaved
        self.saving = saving
        self.saved = saved
        self.error = error
        self.retry = retry
    }
}
