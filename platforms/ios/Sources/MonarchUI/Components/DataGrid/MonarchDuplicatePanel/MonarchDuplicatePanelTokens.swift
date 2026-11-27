import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchDuplicatePanel``.
enum MonarchDuplicatePanelTokens {
    /// Padding around the content of the hint box.
    static let padding: CGFloat = MonarchTokens.Space.space4
    /// The gap between the heading, the matches and the footer.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the lines of one match.
    static let lineSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between badges and between buttons.
    static let itemSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the icon and the heading.
    static let iconSpacing: CGFloat = MonarchTokens.Space.space2
    /// The width of the border.
    static let borderWidth: CGFloat = MonarchShapes.borderWidth

    /// The colour roles of the panel.
    struct Appearance: Equatable {
        /// The fill, or `nil` for no fill.
        let background: MonarchColorRole?
        /// The border, or `nil` for no border.
        let border: MonarchColorRole?
        /// The icon beside the heading, or `nil` for no icon.
        let icon: MonarchColorRole?
    }

    /// The roles for a variant: the hint is a warning surface, the panel has none.
    static func appearance(variant: MonarchDuplicatePanelVariant) -> Appearance {
        switch variant {
        case .hint:
            let palette = MonarchTonePalette.palette(for: .warning)
            return Appearance(background: palette.subtle, border: palette.border, icon: palette.strong)
        case .panel:
            return Appearance(background: nil, border: nil, icon: nil)
        }
    }

    /// The SF Symbol beside the heading of the hint.
    static var symbol: String { MonarchTonePalette.palette(for: .warning).symbol }

    /// The heading: the app's title, or the built-in one for the number of matches.
    static func heading(count: Int, override: String?) -> String {
        if let override, !override.isEmpty { return override }
        return MonarchDuplicatePanelStrings.title(count: count)
    }

    /// The reasons as one line in the user's locale, or `nil` when there are none.
    static func reasonsText(_ reasons: [String], locale: Locale = .autoupdatingCurrent) -> String? {
        let present = reasons.filter { !$0.isEmpty }
        guard !present.isEmpty else { return nil }
        return present.formatted(.list(type: .and, width: .narrow).locale(locale))
    }

    /// The confidence as text, for example "92% match", or `nil` without a
    /// usable score. Scores outside 0 through 1 are limited to that range.
    static func scoreText(_ score: Double?, locale: Locale = .autoupdatingCurrent) -> String? {
        guard let score, !score.isNaN else { return nil }
        let limited = min(max(score, 0), 1)
        return MonarchDuplicatePanelStrings.score(
            limited.formatted(.percent.precision(.fractionLength(0)).locale(locale))
        )
    }

    /// What VoiceOver reads for an action button of one match: the button
    /// title and the name of the record, so buttons of different matches differ.
    static func actionLabel(action: String, record: String) -> String {
        MonarchDuplicatePanelStrings.actionLabel(action: action, record: record)
    }

    /// What is announced when the hint appears: the heading and the names of the matches.
    static func announcement(heading: String, matches: [MonarchDuplicateMatch], locale: Locale = .autoupdatingCurrent)
        -> String
    {
        let names = matches.map(\.title).formatted(.list(type: .and, width: .narrow).locale(locale))
        return names.isEmpty ? heading : "\(heading): \(names)"
    }
}
