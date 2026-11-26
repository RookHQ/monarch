/// How starter prompts are laid out.
public enum MonarchAiStarterPromptsLayout: Sendable, Hashable, CaseIterable {
    /// Cards in as many columns as fit. One column at the accessibility text sizes.
    case grid
    /// Full-width rows.
    case list
}
