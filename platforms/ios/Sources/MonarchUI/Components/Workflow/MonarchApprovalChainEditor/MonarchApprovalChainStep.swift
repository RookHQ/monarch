/// One step of an approval chain, edited by ``MonarchApprovalChainEditor``.
public struct MonarchApprovalChainStep: Identifiable, Sendable, Hashable {
    /// A stable, unique id.
    public var id: String
    /// The `id` of a ``MonarchApprovalApproverType``.
    public var approver: String
    /// The chosen target (a role key, a person id) for approver types with targets.
    public var target: String?
    /// An optional name shown in timelines, such as "Finance check".
    public var label: String?

    /// A step.
    ///
    /// - Parameters:
    ///   - id: A stable, unique id.
    ///   - approver: The `id` of an approver type.
    ///   - target: The chosen target, for approver types with targets.
    ///   - label: An optional name, such as "Finance check".
    public init(id: String, approver: String, target: String? = nil, label: String? = nil) {
        self.id = id
        self.approver = approver
        self.target = target
        self.label = label
    }
}
