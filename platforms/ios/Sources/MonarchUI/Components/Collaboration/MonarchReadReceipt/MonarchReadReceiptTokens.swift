import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchReadReceipt``.
enum MonarchReadReceiptTokens {
    /// The gap between the mark or the avatars and the text.
    static let spacing: CGFloat = MonarchTokens.Space.space1
    /// The SF Symbol before the text in the text and count displays.
    static let seenSymbol = "checkmark"
    /// The type role of the text.
    static let textStyle: MonarchTextStyle = .caption
    /// The colour of the text and the mark.
    static var foreground: MonarchColorRole { \.textMuted }
    /// The size of the avatars.
    static let avatarSize: MonarchAvatarSize = .sm

    /// The visible text for a display.
    static func visibleText(_ display: MonarchReadReceiptDisplay, count: Int, labels: MonarchReadReceiptLabels)
        -> String
    {
        switch display {
        case .count:
            labels.seenByCount?(count) ?? MonarchReadReceiptStrings.seenByCount(count)
        case .text, .avatars:
            labels.seen ?? MonarchReadReceiptStrings.seen
        }
    }

    /// The readers' names joined in the locale's list format: "Ana, Ben and
    /// Chen" in British English.
    static func joined(_ names: [String], labels: MonarchReadReceiptLabels, locale: Locale = .autoupdatingCurrent)
        -> String
    {
        if let list = labels.list { return list(names) }
        return names.formatted(.list(type: .and).locale(locale))
    }

    /// The full sentence, read by VoiceOver in every display.
    static func sentence(
        _ readers: [MonarchReadReceiptReader],
        labels: MonarchReadReceiptLabels,
        locale: Locale = .autoupdatingCurrent
    ) -> String {
        let names = joined(readers.map(\.name), labels: labels, locale: locale)
        return labels.seenBy?(names) ?? MonarchReadReceiptStrings.seenBy(names)
    }

    /// How many avatars are shown before the "+N" tile: at least one, and
    /// never a "+1" tile.
    static func visibleAvatars(total: Int, maxAvatars: Int) -> Int {
        MonarchOverflow.visibleCount(total: total, maxVisible: max(1, maxAvatars))
    }
}
