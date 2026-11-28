/// One item of a ``MonarchToggleGroup``.
///
/// ```swift
/// MonarchToggleGroupOption(.bold, label: "Bold", systemImage: "bold", isIconOnly: true)
/// MonarchToggleGroupOption("open", label: "Open", count: "12")
/// ```
public struct MonarchToggleGroupOption<Value: Hashable>: Hashable {
    /// The value this item adds to the selection while it is on.
    public let value: Value
    /// The visible text and the spoken name, already localised. Required
    /// also for an icon-only item, because VoiceOver reads it.
    public let label: String
    /// An SF Symbol before the label. Decorative.
    public let systemImage: String?
    /// A count after the label, such as "12", already formatted. Spoken after the label.
    public let count: String?
    /// Whether only the symbol is shown. Ignored without a symbol.
    public let isIconOnly: Bool
    /// Whether this item does not respond.
    public let isDisabled: Bool

    /// An item.
    ///
    /// - Parameters:
    ///   - value: The value this item adds to the selection while it is on.
    ///   - label: The visible text and the spoken name, already localised.
    ///   - systemImage: An SF Symbol before the label.
    ///   - count: A count after the label, already formatted.
    ///   - isIconOnly: Whether only the symbol is shown.
    ///   - isDisabled: Whether this item does not respond.
    public init(
        _ value: Value,
        label: String,
        systemImage: String? = nil,
        count: String? = nil,
        isIconOnly: Bool = false,
        isDisabled: Bool = false
    ) {
        self.value = value
        self.label = label
        self.systemImage = systemImage
        self.count = count
        self.isIconOnly = isIconOnly
        self.isDisabled = isDisabled
    }
}

extension MonarchToggleGroupOption: Sendable where Value: Sendable {}
