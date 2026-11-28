/// One field an AI changed on the person's behalf, for ``MonarchAiUndoBanner``.
public struct MonarchAiChange: Sendable, Hashable {
    /// The field's name, such as "Status".
    public var label: String
    /// The value before the change.
    public var before: String
    /// The value the AI set.
    public var after: String

    /// A change.
    ///
    /// - Parameters:
    ///   - label: The field's name, already localised.
    ///   - before: The value before the change.
    ///   - after: The value the AI set.
    public init(_ label: String, before: String, after: String) {
        self.label = label
        self.before = before
        self.after = after
    }
}
