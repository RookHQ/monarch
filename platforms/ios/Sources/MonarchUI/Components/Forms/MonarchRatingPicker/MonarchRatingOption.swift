/// One point on the scale of a ``MonarchRatingPicker``.
public struct MonarchRatingOption: Identifiable, Sendable, Hashable {
    /// The number stored in the binding.
    public let value: Int
    /// A short meaning, already localised, such as "Meets expectations".
    public let label: String?

    /// The identity of the option, which is its value.
    public var id: Int { value }

    /// A point on the scale.
    ///
    /// - Parameters:
    ///   - value: The number stored in the binding.
    ///   - label: A short meaning, already localised.
    public init(_ value: Int, label: String? = nil) {
        self.value = value
        self.label = label
    }
}
