/// One way the assistant can work, for ``MonarchAiModeSwitcher``: for
/// example Ask, Edit or Agent.
public struct MonarchAiMode: Sendable, Hashable, Identifiable {
    /// A stable id. It is the value of the switcher's selection.
    public let id: String
    /// The short name, already localised. One word is best.
    public let label: String
    /// What changes in this mode, already localised. Shown under the
    /// switcher while the mode is selected.
    public let description: String?
    /// An SF Symbol shown instead of the label when `isIconOnly` is true.
    public let systemImage: String?
    /// Whether the segment shows only `systemImage`. The label stays the
    /// spoken name.
    public let isIconOnly: Bool
    /// Whether the mode cannot be chosen.
    public let isDisabled: Bool

    /// A mode.
    ///
    /// - Parameters:
    ///   - id: A stable id.
    ///   - label: The short name, already localised.
    ///   - description: What changes in this mode, already localised.
    ///   - systemImage: An SF Symbol for an icon-only segment.
    ///   - isIconOnly: Whether the segment shows only the symbol.
    ///   - isDisabled: Whether the mode cannot be chosen.
    public init(
        id: String,
        label: String,
        description: String? = nil,
        systemImage: String? = nil,
        isIconOnly: Bool = false,
        isDisabled: Bool = false
    ) {
        self.id = id
        self.label = label
        self.description = description
        self.systemImage = systemImage
        self.isIconOnly = isIconOnly
        self.isDisabled = isDisabled
    }

    /// The built-in modes Ask, Edit and Agent, with ids `ask`, `edit` and
    /// `agent` and a description of what each may change.
    public static var defaults: [MonarchAiMode] {
        [
            MonarchAiMode(
                id: "ask",
                label: MonarchAiModeSwitcherStrings.ask,
                description: MonarchAiModeSwitcherStrings.askDetail,
                systemImage: "bubble.left"
            ),
            MonarchAiMode(
                id: "edit",
                label: MonarchAiModeSwitcherStrings.edit,
                description: MonarchAiModeSwitcherStrings.editDetail,
                systemImage: "pencil"
            ),
            MonarchAiMode(
                id: "agent",
                label: MonarchAiModeSwitcherStrings.agent,
                description: MonarchAiModeSwitcherStrings.agentDetail,
                systemImage: "gearshape.2"
            ),
        ]
    }
}
