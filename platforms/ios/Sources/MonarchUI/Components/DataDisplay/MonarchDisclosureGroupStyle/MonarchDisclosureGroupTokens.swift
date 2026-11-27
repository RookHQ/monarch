import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchDisclosureGroupStyle``.
enum MonarchDisclosureGroupTokens {
    /// The gap between the label and the chevron.
    static let chevronSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the header and the content.
    static let contentSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The space under open content, so a divider below the group never
    /// touches the text (web: 20).
    static let contentBottomPadding: CGFloat = MonarchTokens.Space.space5
    /// The SF Symbol of the chevron. It points down while collapsed.
    /// Padding above and below the header, so the header row is 56 points
    /// high at the default text size, as the web accordion trigger.
    static let headerVerticalPadding: CGFloat = MonarchTokens.Space.space1_5
    /// The type role of the content: 14 points, under the 16 point header.
    static let contentTextStyle: MonarchTextStyle = .label
    /// The colour of the content text: muted, so the header leads.
    static var contentColor: MonarchColorRole { \.textMuted }
    static let chevronSymbol = "chevron.down"
    /// How far the chevron turns when the group is expanded, in degrees.
    static let expandedRotation: Double = 180
    /// The type role of the label.
    static let textStyle: MonarchTextStyle = .body
    /// The weight of the label: medium, as the web trigger (16 / 500).
    static let labelWeight: Font.Weight = .medium

    /// The role of the label for a state.
    static func label(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textDefault : \.textDisabled
    }

    /// The role of the chevron for a state.
    static func chevron(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textMuted : \.textDisabled
    }

    /// The rotation of the chevron for a state, in degrees.
    static func rotation(isExpanded: Bool) -> Double {
        isExpanded ? expandedRotation : 0
    }

    /// What VoiceOver reads as the value of the header.
    static func accessibilityValue(isExpanded: Bool, expandedLabel: String?, collapsedLabel: String?) -> String {
        if isExpanded {
            expandedLabel ?? MonarchDisclosureGroupStrings.expanded
        } else {
            collapsedLabel ?? MonarchDisclosureGroupStrings.collapsed
        }
    }
}
