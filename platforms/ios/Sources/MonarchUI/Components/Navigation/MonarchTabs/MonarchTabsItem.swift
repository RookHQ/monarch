/// One tab of ``MonarchTabs``.
///
/// ```swift
/// MonarchTabsItem("Open", value: Filter.open, count: "12")
/// ```
public struct MonarchTabsItem<Value: Hashable>: Identifiable {
    /// The value the selection takes when the tab is chosen.
    public let value: Value
    /// The label, already localised.
    public let title: String
    /// A count shown after the label, or `nil` for none.
    public let count: String?
    /// The colour of the count.
    public let countTone: MonarchTone
    /// Whether the tab cannot be chosen.
    public let isDisabled: Bool

    /// The identity of the tab, which is its value.
    public var id: Value { value }

    /// A tab.
    ///
    /// - Parameters:
    ///   - title: The label, already localised.
    ///   - value: The value the selection takes when the tab is chosen. Unique in the strip.
    ///   - count: A count shown after the label, for example open items. It is read with the
    ///     label, so pass "12 new" when the number alone is ambiguous.
    ///   - countTone: The colour of the count.
    ///   - isDisabled: Whether the tab cannot be chosen.
    public init(
        _ title: String,
        value: Value,
        count: String? = nil,
        countTone: MonarchTone = .neutral,
        isDisabled: Bool = false
    ) {
        self.title = title
        self.value = value
        self.count = count
        self.countTone = countTone
        self.isDisabled = isDisabled
    }
}

extension MonarchTabsItem: Sendable where Value: Sendable {}
