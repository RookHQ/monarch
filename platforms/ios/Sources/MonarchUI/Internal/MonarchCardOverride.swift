import SwiftUI

/// A fill, a border colour and a border width a parent sets for the ``MonarchCard`` inside it: the AI tint of a
/// card that belongs to an agent, the chosen card of a picker. Colours are roles, so every theme resolves them.
struct MonarchCardOverride: Equatable, @unchecked Sendable {
    var fill: MonarchColorRole?
    var border: MonarchColorRole?
    var borderWidth: CGFloat?

    /// The faint AI border of an agent's card (plan, onboarding tour, task progress).
    static let aiBorder = MonarchCardOverride(fill: nil, border: \.aiBorder, borderWidth: nil)
}

private struct MonarchCardOverrideKey: EnvironmentKey {
    static let defaultValue: MonarchCardOverride? = nil
}

extension EnvironmentValues {
    var monarchCardOverride: MonarchCardOverride? {
        get { self[MonarchCardOverrideKey.self] }
        set { self[MonarchCardOverrideKey.self] = newValue }
    }
}

extension View {
    /// Draws the ``MonarchCard`` inside this view with another fill and border.
    func monarchCardOverride(_ override: MonarchCardOverride?) -> some View {
        environment(\.monarchCardOverride, override)
    }
}

private struct MonarchLeadingEdgeModifier: ViewModifier {
    let role: MonarchColorRole?
    let width: CGFloat

    @Environment(\.monarchTheme) private var theme

    func body(content: Content) -> some View {
        content.overlay {
            if let role {
                HStack(spacing: 0) {
                    Rectangle().fill(theme.colors[role: role]).frame(width: width)
                    Spacer(minLength: 0)
                }
                .clipShape(RoundedRectangle(cornerRadius: MonarchCardTokens.cornerRadius, style: .continuous))
                .allowsHitTesting(false)
                .accessibilityHidden(true)
            }
        }
    }
}

extension View {
    /// A coloured edge along the leading side of a card, as the web marks a card that belongs to the AI or that
    /// needs attention. `nil` draws nothing.
    func monarchLeadingEdge(_ role: MonarchColorRole?, width: CGFloat = 3) -> some View {
        modifier(MonarchLeadingEdgeModifier(role: role, width: width))
    }
}
