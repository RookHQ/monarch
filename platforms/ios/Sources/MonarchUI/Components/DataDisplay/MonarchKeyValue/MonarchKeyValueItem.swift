/// One label and value pair of a ``MonarchKeyValue``.
public struct MonarchKeyValueItem: Sendable, Hashable {
    /// The key, shown muted before the value, such as "Region". Already localised.
    public let label: String
    /// The value. `nil` or an empty string shows a dash read as "Not set".
    public let value: String?
    /// The text a copy button copies, or `nil` for no copy button.
    public let copyValue: String?
    /// Whether the value is drawn in the monospaced face, for ids and keys.
    public let isMonospaced: Bool

    /// A pair.
    ///
    /// - Parameters:
    ///   - label: The key, already localised.
    ///   - value: The value. `nil` or empty shows a dash.
    ///   - isCopyable: Whether a copy button copies the value.
    ///   - copyValue: Something else to copy, such as the full id behind a
    ///     shortened one. Adds a copy button on its own.
    ///   - isMonospaced: Whether the value uses the monospaced face.
    public init(
        _ label: String,
        value: String?,
        isCopyable: Bool = false,
        copyValue: String? = nil,
        isMonospaced: Bool = false
    ) {
        self.label = label
        self.value = value
        self.copyValue = copyValue ?? (isCopyable ? value : nil)
        self.isMonospaced = isMonospaced
    }
}
