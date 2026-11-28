import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchEmptyState`` and ``MonarchErrorState``.
enum MonarchEmptyStateTokens {
    /// The gap between the words of a tip and its key cap.
    static let tipSpacing: CGFloat = MonarchTokens.Space.space1

    /// A tip split around the first whole-word occurrence of `key`, or `nil` when it does not appear.
    static func splitTip(_ tip: String, key: String) -> (before: String, key: String, after: String)? {
        guard !key.isEmpty else { return nil }
        var search = tip.startIndex
        while let range = tip.range(of: key, range: search..<tip.endIndex) {
            let startsWord = range.lowerBound == tip.startIndex || !tip[tip.index(before: range.lowerBound)].isLetter
            let endsWord = range.upperBound == tip.endIndex || !tip[range.upperBound].isLetter
            if startsWord && endsWord {
                return (
                    String(tip[..<range.lowerBound]).trimmingCharacters(in: .whitespaces),
                    key,
                    String(tip[range.upperBound...]).trimmingCharacters(in: .whitespaces)
                )
            }
            search = range.upperBound
        }
        return nil
    }

    /// The point size of the icon glyph before Dynamic Type is applied.
    static func iconSize(_ size: MonarchEmptyStateSize) -> CGFloat {
        switch size {
        case .sm: MonarchTokens.Space.space4
        case .md: MonarchTokens.Space.space5
        case .lg: MonarchTokens.Space.space6
        }
    }

    /// The side of the rounded tile behind the glyph, as the web empty state
    /// (40 points at the default size).
    static func tileSize(_ size: MonarchEmptyStateSize) -> CGFloat {
        switch size {
        case .sm: MonarchTokens.Space.space8
        case .md: MonarchTokens.Space.space10
        case .lg: MonarchTokens.Space.space12
        }
    }

    /// The fill of the tile behind the glyph: a quiet neutral step, or the
    /// success tint for a done state.
    static func tileRole(kind: MonarchEmptyStateKind) -> MonarchColorRole {
        switch kind {
        case .firstRun, .filtered: \.accentSubtle
        case .success: \.successSubtle
        }
    }

    /// The gap between the title and the message, as the web (8 points).
    static let textSpacing: CGFloat = MonarchTokens.Space.space2
    /// The extra gap above the actions.
    static let actionsTopSpacing: CGFloat = MonarchTokens.Space.space2
    /// The widest the text column grows, as the web (448 points).
    static let maxWidth: CGFloat = 448
    /// Padding at the sides.
    static let horizontalPadding: CGFloat = MonarchTokens.Space.space4
    /// Padding above and below: 32 points, 80 for a page-sized state.
    static func verticalPadding(_ size: MonarchEmptyStateSize) -> CGFloat {
        switch size {
        case .sm: MonarchTokens.Space.space4
        case .md: MonarchTokens.Space.space8
        case .lg: MonarchTokens.Space.space20
        }
    }

    /// The gap under the tile, before the title.
    static let tileBottomSpacing: CGFloat = MonarchTokens.Space.space2

    /// The type role of the title.
    static func titleStyle(_ size: MonarchEmptyStateSize) -> MonarchTextStyle {
        switch size {
        case .sm, .md: .bodyStrong
        case .lg: .heading
        }
    }

    /// The type role of the message.
    static func messageStyle(_ size: MonarchEmptyStateSize) -> MonarchTextStyle {
        switch size {
        case .sm: .caption
        case .md: .label
        case .lg: .body
        }
    }

    /// The size of the buttons.
    static func buttonSize(_ size: MonarchEmptyStateSize) -> MonarchButtonSize {
        switch size {
        case .sm: .sm
        case .md: .md
        case .lg: .lg
        }
    }

    /// Whether the state fills the height it is offered and centres itself.
    static func fillsHeight(_ size: MonarchEmptyStateSize) -> Bool {
        size == .lg
    }

    /// The gap between the buttons, the tip and the request id.
    static let actionSpacing: CGFloat = MonarchTokens.Space.space2
    /// The colour role of the title.
    static var title: MonarchColorRole { \.textDefault }
    /// The colour role of the message and the tip.
    static var message: MonarchColorRole { \.textMuted }

    /// The SF Symbol: the app's, or the one of the kind.
    static func symbol(kind: MonarchEmptyStateKind, override: String?) -> String {
        if let override, !override.isEmpty { return override }
        return switch kind {
        case .firstRun: "tray"
        case .filtered: "line.3.horizontal.decrease.circle"
        case .success: "checkmark.circle"
        }
    }

    /// The colour role of the icon of a kind.
    static func iconRole(kind: MonarchEmptyStateKind) -> MonarchColorRole {
        switch kind {
        case .firstRun, .filtered: \.accentStrong
        case .success: \.successStrong
        }
    }

    /// Whether a button is shown for a title and an action: both are needed.
    static func showsAction(title: String?, hasHandler: Bool) -> Bool {
        title != nil && hasHandler
    }

    /// Whether the clear-filters button is shown: only for a filtered view
    /// whose app can clear the filters.
    static func showsClearFilters(kind: MonarchEmptyStateKind, hasHandler: Bool) -> Bool {
        kind == .filtered && hasHandler
    }
}
