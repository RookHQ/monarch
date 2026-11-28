/// One group of a ``MonarchFacetedFilterPanel``, such as "Status" or "File size".
public struct MonarchFacet: Identifiable, Hashable, Sendable {
    /// The key of the facet in the selection. Unique within the panel.
    public let id: String
    /// The group heading, already localised.
    public let label: String
    /// Checkboxes or a range.
    public let kind: MonarchFacetKind
    /// Whether the group starts expanded.
    public let isInitiallyExpanded: Bool

    /// A facet.
    ///
    /// - Parameters:
    ///   - id: The key of the facet in the selection.
    ///   - label: The group heading, already localised.
    ///   - kind: Checkboxes or a range.
    ///   - isInitiallyExpanded: Whether the group starts expanded.
    public init(id: String, label: String, kind: MonarchFacetKind, isInitiallyExpanded: Bool = true) {
        self.id = id
        self.label = label
        self.kind = kind
        self.isInitiallyExpanded = isInitiallyExpanded
    }

    /// A facet of checkboxes.
    ///
    /// - Parameters:
    ///   - id: The key of the facet in the selection.
    ///   - label: The group heading, already localised.
    ///   - options: The choices, in order.
    ///   - maxVisible: How many choices are shown before "Show more".
    ///   - isInitiallyExpanded: Whether the group starts expanded.
    /// - Returns: A facet of kind ``MonarchFacetKind/options(_:maxVisible:)``.
    public static func options(
        id: String,
        label: String,
        options: [MonarchFacetOption],
        maxVisible: Int = 6,
        isInitiallyExpanded: Bool = true
    ) -> MonarchFacet {
        MonarchFacet(
            id: id,
            label: label,
            kind: .options(options, maxVisible: maxVisible),
            isInitiallyExpanded: isInitiallyExpanded
        )
    }

    /// A facet with a minimum and a maximum.
    ///
    /// - Parameters:
    ///   - id: The key of the facet in the selection.
    ///   - label: The group heading, already localised.
    ///   - bounds: The lowest and highest values.
    ///   - step: The distance between allowed values.
    ///   - unit: A unit shown after the values, such as "MB", already localised.
    ///   - isInitiallyExpanded: Whether the group starts expanded.
    /// - Returns: A facet of kind ``MonarchFacetKind/range(_:step:unit:)``.
    public static func range(
        id: String,
        label: String,
        bounds: ClosedRange<Double>,
        step: Double = 1,
        unit: String? = nil,
        isInitiallyExpanded: Bool = true
    ) -> MonarchFacet {
        MonarchFacet(
            id: id,
            label: label,
            kind: .range(bounds, step: step, unit: unit),
            isInitiallyExpanded: isInitiallyExpanded
        )
    }
}
