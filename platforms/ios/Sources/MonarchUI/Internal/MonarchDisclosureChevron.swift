import SwiftUI

/// The chevron of a tree row that opens and closes its children: the goal
/// tree, the subagent tree, the trace view. It points forward when closed
/// and turns a quarter toward the children when open, mirrored in
/// right-to-left layouts because rotation is not.
struct MonarchDisclosureChevron: View {
    let isExpanded: Bool

    @Environment(\.monarchTheme) private var theme
    @Environment(\.layoutDirection) private var layoutDirection

    /// The SF Symbol of the chevron.
    nonisolated static let symbol = "chevron.forward"
    /// How far the chevron turns when open, in degrees.
    nonisolated static let openRotation: Double = 90

    var body: some View {
        MonarchIcon(systemName: Self.symbol, size: .sm)
            .foregroundStyle(theme.colors.textMuted)
            .rotationEffect(Self.rotation(isExpanded: isExpanded, layoutDirection: layoutDirection))
            .monarchAnimation(.fast, value: isExpanded)
    }

    /// The chevron's turn for a state and direction.
    nonisolated static func rotation(isExpanded: Bool, layoutDirection: LayoutDirection) -> Angle {
        guard isExpanded else { return .zero }
        return .degrees(layoutDirection == .rightToLeft ? -openRotation : openRotation)
    }
}
