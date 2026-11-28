/// One labelled row of a ``MonarchEventPopover``: hosts, a location, a reference.
public struct MonarchEventPopoverDetail: Sendable, Hashable {
    /// What the row is, such as "Location". Shown before the value, or only
    /// spoken when the row has an icon.
    public let label: String
    /// The value, already formatted.
    public let value: String
    /// A decorative SF Symbol shown instead of the visible label.
    public let systemImage: String?

    /// A detail row.
    ///
    /// - Parameters:
    ///   - label: What the row is, already localised.
    ///   - value: The value, already formatted.
    ///   - systemImage: A decorative SF Symbol shown instead of the visible label.
    public init(_ label: String, value: String, systemImage: String? = nil) {
        self.label = label
        self.value = value
        self.systemImage = systemImage
    }
}
