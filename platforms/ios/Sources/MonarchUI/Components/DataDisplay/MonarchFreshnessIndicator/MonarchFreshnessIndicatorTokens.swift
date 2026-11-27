import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and rules for ``MonarchFreshnessIndicator``.
enum MonarchFreshnessIndicatorTokens {
    /// The diameter of the dot before Dynamic Type is applied.
    static let dotSize: CGFloat = MonarchTokens.Space.space2
    /// The gap between the dot and the text.
    static let dotSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// The gap between the text and the refresh button.
    static let actionSpacing: CGFloat = MonarchTokens.Space.space2
    /// Padding before and after the content of the banner.
    static let bannerHorizontalPadding: CGFloat = MonarchTokens.Space.space3
    /// Padding above and below the content of the banner.
    static let bannerVerticalPadding: CGFloat = MonarchTokens.Space.space1
    /// The type role of the text.
    static let textStyle: MonarchTextStyle = .caption
    /// The SF Symbol of the refresh button.
    static let refreshSymbol = "arrow.clockwise"
    /// The visible separator between the parts of the line.
    static let separator = " · "
    /// The separator VoiceOver reads between the parts: a pause.
    static let spokenSeparator = ", "
    /// The longest wait between two redraws of the relative time, in seconds.
    static let tickInterval: TimeInterval = 60
    /// The text colour role.
    static var text: MonarchColorRole { \.textMuted }
    /// The fill of the banner.
    static var bannerFill: MonarchColorRole { \.bgSubtle }
    /// The border of the banner.
    static var bannerBorder: MonarchColorRole { \.borderSubtle }

    /// One part of the line, with the colour role that stands out from the
    /// muted text, if any.
    struct Part: Equatable {
        /// The words.
        let text: String
        /// The colour role, or `nil` for the muted text colour.
        let emphasis: MonarchColorRole?
    }

    /// The status from the age of the data. Without a date it is `never`.
    static func status(
        updatedAt: Date?,
        now: Date,
        staleAfter: TimeInterval?,
        expiredAfter: TimeInterval?
    ) -> MonarchFreshnessStatus {
        guard let updatedAt else { return .never }
        let age = now.timeIntervalSince(updatedAt)
        if let expiredAfter, age > expiredAfter { return .expired }
        if let staleAfter, age > staleAfter { return .stale }
        return .fresh
    }

    /// The tone of a status: success, warning, danger, info or neutral.
    static func tone(_ status: MonarchFreshnessStatus) -> MonarchTone {
        switch status {
        case .fresh: .success
        case .stale: .warning
        case .expired, .error: .danger
        case .checking: .info
        case .never: .neutral
        }
    }

    /// The colour role of the dot: the solid colour of the tone, or the
    /// subtle text colour for the neutral tone.
    static func dot(_ status: MonarchFreshnessStatus) -> MonarchColorRole {
        let tone = tone(status)
        return tone == .neutral ? \.textSubtle : MonarchTonePalette.palette(for: tone).solid
    }

    /// The colour role of the status words: the strong colour of the tone.
    static func flag(_ status: MonarchFreshnessStatus) -> MonarchColorRole {
        MonarchTonePalette.palette(for: tone(status)).strong
    }

    /// Seconds in a duration.
    static func seconds(_ duration: Duration?) -> TimeInterval? {
        guard let duration else { return nil }
        let parts = duration.components
        return TimeInterval(parts.seconds) + TimeInterval(parts.attoseconds) / 1e18
    }

    /// A date relative to `now` in words, such as "3 minutes ago".
    static func relativeText(_ date: Date, now: Date, locale: Locale) -> String {
        return MonarchRelativeTime.string(date, now: now, locale: locale, allowsFuture: true)
    }

    /// The parts of the line after the source, in order.
    static func parts(
        status: MonarchFreshnessStatus,
        updatedAt: Date?,
        now: Date,
        error: String?,
        details: [String],
        labels: MonarchFreshnessIndicatorLabels,
        locale: Locale = .autoupdatingCurrent
    ) -> [Part] {
        let errorText =
            error.flatMap { $0.isEmpty ? nil : $0 } ?? labels.error ?? MonarchFreshnessIndicatorStrings.error
        let hasTime = updatedAt != nil && status != .never && status != .checking
        var parts: [Part] = []
        if status == .checking {
            parts.append(Part(text: labels.checking ?? MonarchFreshnessIndicatorStrings.checking, emphasis: nil))
        } else if status == .error && !hasTime {
            parts.append(Part(text: errorText, emphasis: flag(.error)))
        } else if let updatedAt, hasTime {
            let time = relativeText(updatedAt, now: now, locale: locale)
            let updated = labels.updated?(time) ?? MonarchFreshnessIndicatorStrings.updated(time)
            parts.append(Part(text: updated, emphasis: nil))
            switch status {
            case .stale:
                parts.append(Part(text: labels.stale ?? MonarchFreshnessIndicatorStrings.stale, emphasis: flag(.stale)))
            case .expired:
                parts.append(
                    Part(text: labels.expired ?? MonarchFreshnessIndicatorStrings.expired, emphasis: flag(.expired))
                )
            case .error:
                parts.append(Part(text: errorText, emphasis: flag(.error)))
            case .fresh, .never, .checking:
                break
            }
        } else {
            parts.append(Part(text: labels.never ?? MonarchFreshnessIndicatorStrings.never, emphasis: nil))
        }
        parts += details.filter { !$0.isEmpty }.map { Part(text: $0, emphasis: nil) }
        return parts
    }

    /// What VoiceOver reads: the source and the parts, with pauses between them.
    static func accessibilityLabel(source: String?, parts: [Part]) -> String {
        let source = source.flatMap { $0.isEmpty ? nil : $0 }
        return ([source].compactMap { $0 } + parts.map(\.text)).joined(separator: spokenSeparator)
    }

    /// What is announced when the status changes after the first draw:
    /// the status words for stale, expired and error, nothing otherwise.
    static func announcement(
        from old: MonarchFreshnessStatus,
        to new: MonarchFreshnessStatus,
        error: String?,
        labels: MonarchFreshnessIndicatorLabels
    ) -> String? {
        guard old != new else { return nil }
        switch new {
        case .stale: return labels.stale ?? MonarchFreshnessIndicatorStrings.stale
        case .expired: return labels.expired ?? MonarchFreshnessIndicatorStrings.expired
        case .error:
            return error.flatMap { $0.isEmpty ? nil : $0 } ?? labels.error ?? MonarchFreshnessIndicatorStrings.error
        case .fresh, .never, .checking: return nil
        }
    }

    /// When the line must be drawn again after `date`: in a minute, so the
    /// relative time stays right, or just after the next limit, so the
    /// status changes on time, whichever is first.
    static func nextUpdate(after date: Date, updatedAt: Date?, limits: [TimeInterval]) -> Date {
        let tick = date.addingTimeInterval(tickInterval)
        guard let updatedAt else { return tick }
        let boundaries = limits.map { updatedAt.addingTimeInterval($0 + 1) }.filter { $0 > date }
        return ([tick] + boundaries).min() ?? tick
    }
}

/// Redraws a freshness indicator every minute and at each limit.
struct MonarchFreshnessSchedule: TimelineSchedule {
    /// When the data was last updated.
    let updatedAt: Date?
    /// The stale and expired limits, in seconds.
    let limits: [TimeInterval]

    /// The redraw dates from `startDate`.
    func entries(from startDate: Date, mode: TimelineScheduleMode) -> Entries {
        Entries(upcoming: startDate, updatedAt: updatedAt, limits: limits)
    }

    /// An endless sequence of redraw dates.
    struct Entries: Sequence, IteratorProtocol {
        var upcoming: Date
        let updatedAt: Date?
        let limits: [TimeInterval]

        mutating func next() -> Date? {
            let current = upcoming
            upcoming = MonarchFreshnessIndicatorTokens.nextUpdate(after: current, updatedAt: updatedAt, limits: limits)
            return current
        }
    }
}
