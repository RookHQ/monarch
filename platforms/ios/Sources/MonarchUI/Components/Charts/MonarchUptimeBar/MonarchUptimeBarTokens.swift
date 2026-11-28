import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchUptimeBar``.
enum MonarchUptimeBarTokens {
    /// The gap between the header, the bar, the footer and the legend.
    static let spacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the label and the description.
    static let headingSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between two segments.
    static let segmentGap: CGFloat = MonarchTokens.Space.space0_5
    /// The corner radius of a segment.
    static let segmentRadius: CGFloat = MonarchTokens.Space.space0_5
    /// The width of the ring around the inspected segment.
    static let ringWidth: CGFloat = MonarchTokens.Space.space0_5
    /// The padding inside the details popover.
    static let cardPadding: CGFloat = MonarchTokens.Space.space3
    /// The gap between lines of the details popover.
    static let cardSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// The gap between a marker and its text.
    static let markerSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// The gap between legend entries.
    static let legendSpacing: CGFloat = MonarchTokens.Space.space3
    /// The fill of the ring around the inspected segment.
    static var ringRole: MonarchColorRole { \.textDefault }
    /// The outline of a period with no data.
    static var noDataBorderRole: MonarchColorRole { \.borderDefault }
    /// The states in the order of the legend and the summary.
    static let order: [MonarchUptimeBarStatus] = MonarchUptimeBarStatus.allCases

    /// The height of the segments.
    static func height(_ size: MonarchUptimeBarSize) -> CGFloat {
        switch size {
        case .sm: MonarchTokens.Space.space5
        case .md: MonarchTokens.Space.space8
        }
    }

    /// The fill of a state. Partial outage uses the strong warning colour; no data is the muted background with an outline.
    static func role(_ status: MonarchUptimeBarStatus) -> MonarchColorRole {
        switch status {
        case .operational: \.successSolid
        case .degraded: \.warningSolid
        case .partial: \.warningStrong
        case .down: \.dangerSolid
        case .maintenance: \.infoSolid
        case .noData: \.bgMuted
        }
    }

    /// The fill colour of a state: the colour of its role, as on the web.
    static func fill(_ status: MonarchUptimeBarStatus, colors: MonarchColors) -> Color {
        colors[role: role(status)]
    }

    /// The state of a segment: its own, or one derived from its uptime.
    static func status(_ segment: MonarchUptimeBarSegment, thresholds: MonarchUptimeBarThresholds)
        -> MonarchUptimeBarStatus
    {
        if let status = segment.status { return status }
        guard let uptime = segment.uptime, uptime.isFinite else { return .noData }
        if uptime >= thresholds.operational { return .operational }
        if uptime >= thresholds.degraded { return .degraded }
        return .down
    }

    /// The overall uptime: the given value, or the mean of the measured
    /// segments, or `nil` when nothing was measured.
    static func overall(_ segments: [MonarchUptimeBarSegment], given: Double?) -> Double? {
        if let given, given.isFinite { return given }
        let measured = segments.compactMap(\.uptime).filter(\.isFinite)
        guard !measured.isEmpty else { return nil }
        return measured.reduce(0, +) / Double(measured.count)
    }

    /// How many segments are in each state.
    static func counts(_ statuses: [MonarchUptimeBarStatus]) -> [MonarchUptimeBarStatus: Int] {
        statuses.reduce(into: [:]) { $0[$1, default: 0] += 1 }
    }

    /// A percentage (0 to 100) written with a fixed number of decimals in the locale.
    static func percent(_ value: Double, digits: Int, locale: Locale) -> String {
        let digits = max(0, digits)
        return (value / 100).formatted(.percent.precision(.fractionLength(digits)).locale(locale))
    }

    /// The name of a state, from the labels or built in.
    static func name(_ status: MonarchUptimeBarStatus, labels: MonarchUptimeBarLabels) -> String {
        labels.statusNames[status] ?? MonarchUptimeBarStrings.status(status)
    }

    /// The summary VoiceOver reads for the bar: the number of periods, the
    /// count of each state present, and the overall uptime.
    static func summary(
        count: Int,
        counts: [MonarchUptimeBarStatus: Int],
        overall: String?,
        labels: MonarchUptimeBarLabels,
        locale: Locale
    ) -> String {
        if let custom = labels.summary { return custom(count, counts, overall) }
        var sentences = [MonarchUptimeBarStrings.periods(count.formatted(.number.locale(locale)))]
        let states = order.compactMap { status -> String? in
            guard let number = counts[status], number > 0 else { return nil }
            return "\(name(status, labels: labels)) \(number.formatted(.number.locale(locale)))"
        }
        if !states.isEmpty { sentences.append(states.joined(separator: ", ")) }
        if let overall { sentences.append("\(labels.uptime ?? MonarchUptimeBarStrings.uptime) \(overall)") }
        return sentences.joined(separator: ". ") + "."
    }

    /// The details of one segment as one line, read by VoiceOver while it is
    /// inspected: "Mar 4: Degraded, Uptime 99.52%, Checks 1,440, Slow API".
    static func segmentText(
        _ segment: MonarchUptimeBarSegment,
        status: MonarchUptimeBarStatus,
        digits: Int,
        labels: MonarchUptimeBarLabels,
        locale: Locale
    ) -> String {
        var parts = [name(status, labels: labels)]
        if let uptime = segment.uptime, uptime.isFinite {
            parts.append(
                "\(labels.uptime ?? MonarchUptimeBarStrings.uptime) \(percent(uptime, digits: digits, locale: locale))"
            )
        }
        parts += segment.details.map { "\($0.label) \($0.value)" }
        if let note = segment.note, !note.isEmpty { parts.append(note) }
        return "\(segment.label): \(parts.joined(separator: ", "))"
    }

    /// The segment under a touch at `x` points from the left edge of a bar
    /// `width` wide. Segments run from the leading edge, so in right-to-left
    /// layouts the first one is on the right.
    static func index(at x: CGFloat, width: CGFloat, count: Int, isRightToLeft: Bool) -> Int? {
        guard count > 0, width > 0, x >= 0, x <= width else { return nil }
        let fromLeading = isRightToLeft ? width - x : x
        return min(count - 1, max(0, Int(fromLeading / width * CGFloat(count))))
    }

    /// The inspected segment after a VoiceOver swipe: up (increment) moves
    /// to the newer period, down to the older one. With none inspected, up
    /// starts at the oldest and down at the newest.
    static func adjusted(_ current: Int?, increment: Bool, count: Int) -> Int? {
        guard count > 0 else { return nil }
        guard let current else { return increment ? 0 : count - 1 }
        return min(count - 1, max(0, current + (increment ? 1 : -1)))
    }
}
