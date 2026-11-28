/// Replacements for the built-in strings of ``MonarchAiGenerativeUi``.
///
/// Every property is `nil` by default, which keeps the built-in string.
public struct MonarchAiGenerativeUiLabels: Sendable, Hashable {
    /// Shown instead of a component that is not in the registry.
    /// Built in: "This part of the answer can’t be shown here."
    public var unknown: String?
    /// Shown when the props are not valid. Built in: "This part of the answer had data that couldn’t be shown."
    public var invalid: String?
    /// Shown when the component fails to build. Built in: "This part of the answer couldn’t be displayed."
    public var failed: String?
    /// The spoken name of the progress indicator while props stream. Built in: "Loading".
    public var loading: String?

    /// Replacements for built-in strings. Pass only the ones to replace.
    public init(unknown: String? = nil, invalid: String? = nil, failed: String? = nil, loading: String? = nil) {
        self.unknown = unknown
        self.invalid = invalid
        self.failed = failed
        self.loading = loading
    }
}
