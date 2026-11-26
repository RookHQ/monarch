import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchSectionHeader``.
enum MonarchSectionHeaderTokens {
    /// The narrowest the title column of a card header gets beside its actions before they go under it.
    static let leastTitleWidth: CGFloat = 160
    /// The gap between the title, the description and the meta line.
    static let textSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between the title and the progress indicator.
    static let titleSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the text and the actions when they share a row.
    static let barSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the text and the actions when the actions are below.
    static let stackedSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between two actions.
    static let actionSpacing: CGFloat = MonarchTokens.Space.space2
    /// The weight of the title at every size.
    static let titleWeight = MonarchTextStyleSpec.fontWeight(MonarchTokens.FontWeight.semibold)
    /// The type role of the description.
    static let descriptionStyle: MonarchTextStyle = .label
    /// The weight of the description.
    static let descriptionWeight = MonarchTextStyleSpec.fontWeight(MonarchTokens.FontWeight.regular)
    /// The type role of the meta line.
    static let metaStyle: MonarchTextStyle = .caption
    /// The colour of the title.
    static var titleColor: MonarchColorRole { \.textDefault }
    /// The colour of the description and the meta line.
    static var supportingColor: MonarchColorRole { \.textMuted }

    /// The type role of the title.
    static func titleStyle(_ size: MonarchSectionHeaderSize) -> MonarchTextStyle {
        switch size {
        case .sm: .label
        case .md: .body
        }
    }

    /// Whether the actions go below the text instead of beside it. At the
    /// accessibility text sizes a shared row leaves the title too little width.
    static func stacksActions(at dynamicTypeSize: DynamicTypeSize) -> Bool {
        dynamicTypeSize.isAccessibilitySize
    }

    /// What VoiceOver reads as the value of the title: the busy label while
    /// the content is loading, otherwise nothing.
    static func accessibilityValue(isBusy: Bool, busyLabel: String?) -> String {
        isBusy ? (busyLabel ?? MonarchSectionHeaderStrings.busy) : ""
    }
}
