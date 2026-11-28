import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchPostCard``.
enum MonarchPostCardTokens {
    /// The gap between the parts of the meta line and between counts.
    static let metaSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between an icon and its text.
    static let iconSpacing: CGFloat = MonarchTokens.Space.space1
    /// The size of the author's avatar.
    static let avatarSize: MonarchAvatarSize = .sm
    /// The type role of the title.
    static let titleStyle: MonarchTextStyle = .subheading
    /// The type role of the meta line, the excerpt and the counts.
    static let metaStyle: MonarchTextStyle = .caption
    /// The type role of the excerpt.
    static let excerptStyle: MonarchTextStyle = .body
    /// The colour of the title.
    static var titleColor: MonarchColorRole { \.textDefault }
    /// The colour of the meta line and the counts.
    static var metaColor: MonarchColorRole { \.textMuted }
    /// The colour of the excerpt.
    static var excerptColor: MonarchColorRole { \.textMuted }
    /// The SF Symbol of the pinned marker.
    static let pinnedSymbol = "pin.fill"
    /// The SF Symbol of the answered badge.
    static let answeredSymbol = "checkmark"
    /// The SF Symbol of the reaction count.
    static let reactionsSymbol = "hand.thumbsup"
    /// The SF Symbol of the comment count.
    static let commentsSymbol = "bubble.left"
    /// The longest excerpt by default, in characters.
    static let defaultExcerptLength = 220
    /// A cut at a word boundary is kept only past this share of the limit.
    static let wordBoundaryRatio = 0.6

    /// Shortens text to at most `max` characters at a word boundary and adds
    /// an ellipsis. Text that already fits is returned trimmed and unchanged.
    static func excerpt(_ text: String, max: Int) -> String {
        let plain = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard plain.count > max, max > 0 else { return plain }
        let cut = String(plain.prefix(max))
        if let space = cut.lastIndex(of: " "),
            Double(cut.distance(from: cut.startIndex, to: space)) > Double(max) * wordBoundaryRatio
        {
            return String(cut[..<space]).trimmingCharacters(in: .whitespaces) + "…"
        }
        return cut.trimmingCharacters(in: .whitespaces) + "…"
    }

    /// "3 hours ago", "yesterday": the publication time relative to `now`.
    static func relativeTime(_ date: Date, now: Date, locale: Locale) -> String {
        MonarchRelativeTime.string(date, now: now, locale: locale, allowsFuture: true)
    }

    /// The spoken reaction count.
    static func reactions(_ count: Int, labels: MonarchPostCardLabels) -> String {
        labels.reactions?(count) ?? MonarchPostCardStrings.reactions(count)
    }

    /// The spoken comment count.
    static func comments(_ count: Int, labels: MonarchPostCardLabels) -> String {
        labels.comments?(count) ?? MonarchPostCardStrings.comments(count)
    }
}
