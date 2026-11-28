import MonarchTokens
import SwiftUI

/// Component tokens and rules for ``MonarchToggleBlock``.
enum MonarchToggleBlockTokens {
    /// The gap between the chevron and the summary.
    static let chevronSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the summary and the content.
    static let contentSpacing: CGFloat = MonarchTokens.Space.space1
    /// The width the chevron takes before Dynamic Type is applied; the
    /// content is indented by it and the spacing, so it lines up with the summary.
    static let chevronWidth: CGFloat = MonarchTokens.Space.space6
    /// The type role of the summary.
    static let textStyle: MonarchTextStyle = .label

    /// The chevron: forward while collapsed, down while expanded. Two
    /// symbols instead of a rotation, so the forward chevron points the
    /// right way in right-to-left layouts without a turn that depends on
    /// the direction.
    static func chevronSymbol(isExpanded: Bool) -> String {
        isExpanded ? "chevron.down" : "chevron.forward"
    }

    /// The role of the summary for a state.
    static func summary(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textDefault : \.textDisabled
    }

    /// The role of the chevron for a state.
    static func chevron(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textMuted : \.textDisabled
    }
}
