/// One next step offered by ``MonarchAiSuggestedActions``.
@_spi(Experimental) public struct MonarchAiSuggestedAction: Identifiable, Sendable, Hashable {
    /// The stable id, passed to `onRun` and `onDismiss`.
    public var id: String
    /// What the action does, as a verb phrase: "Send the renewal reminder".
    public var label: String
    /// Why it is suggested or what it will change.
    public var description: String?
    /// Whether the person confirms it in a dialog before it runs. Implied by a destructive risk.
    public var requiresApproval: Bool
    /// How much harm running it could do. Medium and above show a tag.
    public var risk: MonarchAiToolCallRisk
    /// What will happen, listed in the approval dialog: "3 people get an email".
    public var consequences: [String]
    /// The confirm button of the approval dialog. Built in: "Run action".
    public var confirmLabel: String?
    /// The status, when the app tracks it. Without it the list tracks it from `onRun`.
    public var status: MonarchAiSuggestedActionStatus?

    /// A suggested action.
    ///
    /// - Parameters:
    ///   - id: The stable id.
    ///   - label: What the action does, as a verb phrase.
    ///   - description: Why it is suggested or what it will change.
    ///   - requiresApproval: Whether the person confirms it before it runs.
    ///   - risk: How much harm running it could do.
    ///   - consequences: What will happen, listed in the approval dialog.
    ///   - confirmLabel: The confirm button of the approval dialog.
    ///   - status: The status, when the app tracks it.
    public init(
        id: String,
        label: String,
        description: String? = nil,
        requiresApproval: Bool = false,
        risk: MonarchAiToolCallRisk = .low,
        consequences: [String] = [],
        confirmLabel: String? = nil,
        status: MonarchAiSuggestedActionStatus? = nil
    ) {
        self.id = id
        self.label = label
        self.description = description
        self.requiresApproval = requiresApproval
        self.risk = risk
        self.consequences = consequences
        self.confirmLabel = confirmLabel
        self.status = status
    }

    /// Whether the action is confirmed in a dialog before it runs: when it
    /// requires approval or its risk is destructive.
    public var needsApproval: Bool { requiresApproval || risk == .destructive }
}
