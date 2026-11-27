import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchLogoCloud``.
enum MonarchLogoCloudTokens {
    /// The gap between the title and the logos.
    static let titleSpacing: CGFloat = MonarchTokens.Space.space5
    /// The gap between logos in a row.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space6
    /// The gap between rows of logos.
    static let rowLineSpacing: CGFloat = MonarchTokens.Space.space4
    /// The gap between grid cells.
    static let gridSpacing: CGFloat = MonarchTokens.Space.space3
    /// The narrowest a grid cell gets.
    static let cellMinimumWidth: CGFloat = MonarchTokens.Space.space24 + MonarchTokens.Space.space12
    /// The padding inside a grid cell.
    static let cellPadding: CGFloat = MonarchTokens.Space.space4
    /// The weight of the title.
    static let titleWeight = MonarchTextStyleSpec.fontWeight(MonarchTokens.FontWeight.semibold)
    /// The weight of a wordmark.
    static let wordmarkWeight = MonarchTextStyleSpec.fontWeight(MonarchTokens.FontWeight.semibold)
    /// The colour of the title.
    static var titleColor: MonarchColorRole { \.textMuted }
    /// The colour of a wordmark.
    static var wordmarkColor: MonarchColorRole { \.textMuted }
    /// The border of a grid cell.
    static var cellBorder: MonarchColorRole { \.borderSubtle }

    /// The height of a logo image for a size.
    static func logoHeight(_ size: MonarchLogoCloudSize) -> CGFloat {
        switch size {
        case .sm: MonarchTokens.Space.space6
        case .md: MonarchTokens.Space.space8
        case .lg: MonarchTokens.Space.space10 + MonarchTokens.Space.space1
        }
    }

    /// The type role of a wordmark for a size.
    static func wordmarkStyle(_ size: MonarchLogoCloudSize) -> MonarchTextStyle {
        switch size {
        case .sm: .label
        case .md: .bodyStrong
        case .lg: .subheading
        }
    }

    /// The spoken name of the list: the title, else the built-in "Organisations".
    static func listLabel(title: String?, override: String?) -> String {
        if let title, !title.isEmpty { return title }
        return override ?? MonarchLogoCloudStrings.listLabel
    }
}
