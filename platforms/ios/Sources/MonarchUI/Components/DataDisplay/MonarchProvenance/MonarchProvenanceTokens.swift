import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and rules for ``MonarchProvenance``.
enum MonarchProvenanceTokens {
    /// The gap between the icon and the text.
    static let iconSpacing: CGFloat = MonarchTokens.Space.space1
    /// Padding before and after the content of the chip.
    static let chipHorizontalPadding: CGFloat = MonarchTokens.Space.space1_5
    /// Padding above and below the content of the chip.
    static let chipVerticalPadding: CGFloat = MonarchTokens.Space.space0_5
    /// The corner radius of the chip.
    static let chipRadius: CGFloat = MonarchTokens.Radius.sm
    /// The type role of the text.
    static let textStyle: MonarchTextStyle = .caption
    /// The visible separator between the source and the time.
    static let separator = " · "
    /// The URL schemes a source link may open.
    static let safeSchemes: Set<String> = ["http", "https", "mailto"]
    /// The fill of the chip.
    static var chipFill: MonarchColorRole { \.bgSubtle }
    /// The border of the chip.
    static var chipBorder: MonarchColorRole { \.borderSubtle }

    /// The colour role of the source: the link colour when it opens a URL,
    /// otherwise the muted text colour of the rest.
    static func source(isLink: Bool) -> MonarchColorRole {
        isLink ? \.textLink : \.textMuted
    }

    /// The colour role of the time and the separator.
    static var text: MonarchColorRole { \.textMuted }

    /// Whether anything is drawn: only with a source.
    static func isVisible(source: String) -> Bool {
        !source.trimmingCharacters(in: .whitespaces).isEmpty
    }

    /// The URL, if it may be opened: http, https and mailto only.
    static func safeURL(_ url: URL?) -> URL? {
        guard let url, let scheme = url.scheme?.lowercased(), safeSchemes.contains(scheme) else { return nil }
        return url
    }

    /// A date relative to `now` in words, such as "2 hours ago".
    static func relativeText(_ date: Date, now: Date, locale: Locale) -> String {
        return MonarchRelativeTime.string(date, now: now, locale: locale, allowsFuture: true)
    }

    /// What VoiceOver reads: "Source: Registry, checked 2 hours ago".
    static func accessibilityLabel(source: String, time: String?, labels: MonarchProvenanceLabels) -> String {
        let spokenSource = labels.source?(source) ?? MonarchProvenanceStrings.source(source)
        guard let time else { return spokenSource }
        let spokenTime = labels.checked?(time) ?? MonarchProvenanceStrings.checked(time)
        return spokenSource + ", " + spokenTime
    }
}
