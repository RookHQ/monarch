import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchStackedProportionBar``.
enum MonarchStackedProportionBarTokens {
    /// The gap between the bar and the legend.
    static let spacing: CGFloat = MonarchTokens.Space.space2_5
    /// The gap between two segments, so neighbours never merge.
    static let gap: CGFloat = MonarchTokens.Space.space0_5
    /// The narrowest a non-zero segment is drawn, so tiny shares stay visible.
    static let minimumSegmentWidth: CGFloat = MonarchTokens.Space.space1
    /// The fill of the track and of the empty remainder.
    static var trackRole: MonarchColorRole { \.bgMuted }
    /// The outline of the remainder's legend marker.
    static var remainderRole: MonarchColorRole { \.borderStrong }
    /// The key of the remainder's legend entry.
    static let remainderKey = "monarch.remainder"

    /// The thickness of the bar.
    static func thickness(_ size: MonarchStackedProportionBarSize) -> CGFloat {
        switch size {
        case .sm: MonarchTokens.Space.space1
        case .md: MonarchTokens.Space.space2
        case .lg: MonarchTokens.Space.space3
        }
    }

    /// The colour of a status tone: the solid intent colour, or the subtle
    /// text colour for neutral (no data, paused).
    static func toneRole(_ tone: MonarchTone) -> MonarchColorRole {
        switch tone {
        case .neutral: \.textSubtle
        case .info: \.infoSolid
        case .success: \.successSolid
        case .warning: \.warningSolid
        case .danger: \.dangerSolid
        case .accent: \.accentSolid
        case .ai: \.aiSolid
        }
    }

    /// Whether the bar is in status mode: every segment has a tone.
    static func isStatusMode(_ segments: [MonarchStackedProportionBarSegment]) -> Bool {
        !segments.isEmpty && segments.allSatisfy { $0.tone != nil }
    }

    /// One part to draw and list.
    struct Part: Equatable {
        /// The key of the segment.
        let key: String
        /// The visible name.
        let label: String
        /// The value, never negative.
        let value: Double
        /// The palette slot, used by the legend in palette mode.
        let color: MonarchChartColor
        /// The fill of the segment and its marker.
        let role: MonarchColorRole
    }

    /// The parts in display order, with values cleaned and colours resolved.
    /// Palette slots follow the position in `segments`, before zeros are
    /// left out, so hiding a part never repaints the others.
    static func parts(_ segments: [MonarchStackedProportionBarSegment], hidesZero: Bool) -> [Part] {
        let statusMode = isStatusMode(segments)
        let parts = segments.enumerated().map { index, segment in
            let color = MonarchChartLegendTokens.color(segment.color, index: index)
            let role: MonarchColorRole =
                if statusMode, let tone = segment.tone { toneRole(tone) } else { MonarchChartLegendTokens.role(color) }
            return Part(
                key: segment.key,
                label: segment.label,
                value: segment.value.isFinite && segment.value > 0 ? segment.value : 0,
                color: color,
                role: role
            )
        }
        return hidesZero ? parts.filter { $0.value > 0 } : parts
    }

    /// The size of the whole: the sum, or `total` when it is larger.
    static func total(sum: Double, given: Double?) -> Double {
        guard let given, given.isFinite else { return sum }
        return max(sum, given)
    }

    /// A share of the whole, written in the locale, with "<1%" for a share
    /// too small to write and ">99%" for one too close to the whole.
    static func share(_ part: Double, of total: Double, digits: Int, locale: Locale) -> String {
        let digits = max(0, digits)
        func percent(_ ratio: Double) -> String {
            ratio.formatted(.percent.precision(.fractionLength(0...digits)).locale(locale))
        }
        guard total > 0, part > 0 else { return percent(0) }
        let ratio = part / total
        let step = pow(10, -Double(digits + 2))
        if ratio < step { return MonarchStackedProportionBarStrings.lessThan(percent(step)) }
        if ratio < 1, ratio > 1 - step { return MonarchStackedProportionBarStrings.moreThan(percent(1 - step)) }
        return percent(ratio)
    }

    /// The text after a legend label, or `nil` for `none`.
    static func legendValue(
        _ value: Double,
        total: Double,
        content: MonarchStackedProportionBarLegendContent,
        digits: Int,
        locale: Locale,
        formatValue: ((Double) -> String)?
    ) -> String? {
        let written = formatValue?(value) ?? value.formatted(.number.locale(locale))
        switch content {
        case .none: return nil
        case .value: return written
        case .percent: return share(value, of: total, digits: digits, locale: locale)
        case .both:
            return MonarchStackedProportionBarStrings.valueAndShare(
                written,
                share(value, of: total, digits: digits, locale: locale)
            )
        }
    }

    /// The widths of the drawn segments and of the remainder in a bar of
    /// `available` points. Every segment is at least `minimum` wide; the
    /// others share what is left in proportion to their values.
    static func widths(
        _ values: [Double],
        remainder: Double,
        available: CGFloat,
        gap: CGFloat,
        minimum: CGFloat
    ) -> (segments: [CGFloat], remainder: CGFloat) {
        let remainder = max(0, remainder)
        let pieces = values.count + (remainder > 0 ? 1 : 0)
        let whole = values.reduce(0, +) + remainder
        guard pieces > 0, available > 0, whole > 0 else { return (values.map { _ in 0 }, 0) }
        let room = max(0, available - gap * CGFloat(pieces - 1))
        let raw = values.map { room * CGFloat($0 / whole) }
        let rawRemainder = room * CGFloat(remainder / whole)
        let small = Set(raw.indices.filter { raw[$0] < minimum })
        let floor = min(minimum, room)
        let flexible = raw.indices.filter { !small.contains($0) }.map { raw[$0] }.reduce(0, +) + rawRemainder
        let scale = flexible > 0 ? max(0, room - CGFloat(small.count) * floor) / flexible : 0
        let segments = raw.indices.map { small.contains($0) ? floor : raw[$0] * scale }
        return (segments, rawRemainder * scale)
    }

    /// What VoiceOver reads for the bar when the legend is hidden or empty:
    /// every entry, as the legend would read them.
    static func accessibilityValue(_ entries: [(label: String, value: String?)]) -> String {
        entries.map { MonarchChartLegendTokens.accessibilityLabel(label: $0.label, value: $0.value) }
            .joined(separator: "; ")
    }
}
