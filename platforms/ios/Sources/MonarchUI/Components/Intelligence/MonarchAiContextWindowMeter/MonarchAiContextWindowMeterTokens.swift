import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchAiContextWindowMeter``.
enum MonarchAiContextWindowMeterTokens {
    /// The gap between the header, the bar, the legend, the notice and the button.
    static let stackSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the heading and the summary.
    static let headerSpacing: CGFloat = MonarchTokens.Space.space2
    /// The height of the bar.
    static let barHeight: CGFloat = MonarchTokens.Space.space2
    /// The width of the bar in the compact form.
    static let compactBarWidth: CGFloat = MonarchTokens.Space.space16
    /// The gap between the bar and the percentage in the compact form.
    static let compactSpacing: CGFloat = MonarchTokens.Space.space2
    /// Padding around the details in the popover.
    static let popoverPadding: CGFloat = MonarchTokens.Space.space4
    /// The least width of the details in the popover.
    static let popoverMinWidth: CGFloat = MonarchTokens.Space.space24 * 3
    /// The track of the bar.
    static var track: MonarchColorRole { \.bgMuted }
    /// The SF Symbol of the notice near the limit.
    static let warningSymbol = "exclamationmark.triangle.fill"
    /// The SF Symbol of the notice when full.
    static let fullSymbol = "exclamationmark.octagon.fill"

    /// One segment of the bar and its legend entry.
    struct Segment: Equatable, Identifiable {
        let id: String
        let label: String
        let tokens: Int
        let color: MonarchChartColor
    }

    /// The segments for a breakdown: the parts in order with the data
    /// colours (the sixth part and later are neutral, as in a chart legend),
    /// then "Other" in neutral for the tokens no part covers. Empty without a
    /// breakdown.
    static func segments(breakdown: [MonarchAiContextWindowPart], used: Int, other: String) -> [Segment] {
        guard !breakdown.isEmpty else { return [] }
        var segments = breakdown.enumerated().map { index, part in
            Segment(
                id: "part-\(index)-\(part.id)",
                label: part.label,
                tokens: max(0, part.tokens),
                color: MonarchChartLegendTokens.color(nil, index: index)
            )
        }
        let covered = segments.reduce(0) { $0 + $1.tokens }
        let total = max(0, used)
        if total > covered {
            segments.append(Segment(id: "other", label: other, tokens: total - covered, color: .neutral))
        }
        return segments
    }

    /// The share of the bar each segment takes, 0 to 1. The whole bar is the
    /// window size, or the total when the segments add up to more.
    static func fractions(_ segments: [Segment], limit: Int) -> [Double] {
        let total = segments.reduce(0) { $0 + $1.tokens }
        let scale = Double(max(limit, total))
        guard scale > 0 else { return segments.map { _ in 0 } }
        return segments.map { Double($0.tokens) / scale }
    }

    /// The share of the window that is used, 0 to 1.
    static func fraction(used: Int, limit: Int) -> Double {
        guard limit > 0 else { return 0 }
        return min(1, Double(max(0, used)) / Double(limit))
    }

    /// The fill of the bar without a breakdown: accent, then warning, then danger.
    static func fill(_ state: MonarchAiContextWindowState) -> MonarchColorRole {
        switch state {
        case .ok: \.accentSolid
        case .warning: \.warningSolid
        case .full: \.dangerSolid
        }
    }

    /// The tone of the notice, or `nil` when there is room.
    static func noticeTone(_ state: MonarchAiContextWindowState) -> MonarchTone? {
        switch state {
        case .ok: nil
        case .warning: .warning
        case .full: .danger
        }
    }

    /// The notice for a state, or `nil` when there is room.
    static func notice(_ state: MonarchAiContextWindowState, labels: MonarchAiContextWindowMeterLabels) -> String? {
        switch state {
        case .ok: nil
        case .warning: labels.warning
        case .full: labels.full
        }
    }

    /// The SF Symbol of the notice.
    static func noticeSymbol(_ state: MonarchAiContextWindowState) -> String {
        state == .full ? fullSymbol : warningSymbol
    }

    /// The summarize button is quiet while there is room and stronger once the meter warns.
    static func buttonVariant(_ state: MonarchAiContextWindowState) -> MonarchButtonVariant {
        state == .ok ? .ghost : .secondary
    }

    /// A token count in the locale: "45,000".
    static func number(_ value: Int, locale: Locale) -> String {
        value.formatted(.number.locale(locale))
    }

    /// A share in the locale, without decimals: "35%".
    static func percent(_ fraction: Double, locale: Locale) -> String {
        fraction.formatted(.percent.precision(.fractionLength(0)).locale(locale))
    }

    /// The summary line, which is also the bar's spoken value:
    /// "45,000 of 128,000 tokens (35%)", or "About …" for an estimate.
    static func amount(
        used: Int,
        limit: Int,
        isApproximate: Bool,
        locale: Locale,
        labels: MonarchAiContextWindowMeterLabels
    ) -> String {
        let exact = labels.amount(
            number(max(0, used), locale: locale),
            number(max(0, limit), locale: locale),
            percent(fraction(used: used, limit: limit), locale: locale)
        )
        return isApproximate ? labels.approximate(exact) : exact
    }

    /// The legend entries of the segments, with each amount as its value.
    static func legendItems(
        _ segments: [Segment],
        locale: Locale,
        labels: MonarchAiContextWindowMeterLabels
    ) -> [MonarchChartLegendItem] {
        segments.map { segment in
            MonarchChartLegendItem(
                segment.id,
                label: segment.label,
                color: segment.color,
                value: labels.tokens(number(segment.tokens, locale: locale))
            )
        }
    }
}
