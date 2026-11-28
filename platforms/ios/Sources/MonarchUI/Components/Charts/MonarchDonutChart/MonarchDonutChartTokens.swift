import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchDonutChart``.
enum MonarchDonutChartTokens {
    /// The outer diameter: 160 points.
    static let defaultSize: CGFloat = MonarchTokens.Space.space20 * 2
    /// The ring's thickness as a share of the diameter.
    static let thicknessRatio: CGFloat = 0.22
    /// The thinnest ring.
    static let minimumThickness: CGFloat = MonarchTokens.Space.space1
    /// The gap between two slices, at the ring.
    static let sliceGap: CGFloat = MonarchTokens.Space.space0_5
    /// The most slices drawn by default: the five data slots and Other.
    static let defaultMaxSegments = 6
    /// The least number of slices, so Other never stands alone.
    static let minimumSegments = 2
    /// The gap between the donut and the legend in a row.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space6
    /// The gap between the donut and the legend in a column.
    static let columnSpacing: CGFloat = MonarchTokens.Space.space4
    /// Room between the ring and the centre text.
    static let centerInset: CGFloat = MonarchTokens.Space.space1
    /// How far the centre text may shrink to fit the hole at large text sizes.
    static let centerMinimumScale: CGFloat = 0.5
    /// The type role of the centre value.
    static let centerValueStyle: MonarchTextStyle = .heading
    /// The type role of the centre label.
    static let centerLabelStyle: MonarchTextStyle = .caption
    /// The key of the folded slice.
    static let otherKey = "__other"

    /// The colour of the ring behind the slices.
    static var trackRole: MonarchColorRole { \.bgMuted }

    /// One slice to draw.
    struct Slice: Hashable, Identifiable {
        /// The key of the segment, or ``otherKey``.
        let key: String
        /// The name.
        let label: String
        /// The amount, 0 or more.
        let value: Double
        /// The colour slot.
        let color: MonarchChartColor

        /// The key.
        var id: String { key }
    }

    /// An amount as drawn: a negative or non-finite value counts as 0.
    static func clean(_ value: Double) -> Double {
        value.isFinite && value > 0 ? value : 0
    }

    /// The segments in drawing order, with clean values and their colour
    /// slots. The default order is the largest first; equal values keep
    /// their given order. Colours follow the position after sorting unless
    /// a segment pins one.
    static func ordered(_ segments: [MonarchDonutChartSegment], sort: MonarchDonutChartSort) -> [Slice] {
        var indexed = Array(segments.enumerated())
        if sort == .descending {
            indexed.sort { lhs, rhs in
                let left = clean(lhs.element.value)
                let right = clean(rhs.element.value)
                return left == right ? lhs.offset < rhs.offset : left > right
            }
        }
        return indexed.enumerated().map { position, entry in
            Slice(
                key: entry.element.key,
                label: entry.element.label,
                value: clean(entry.element.value),
                color: MonarchChartLegendTokens.color(entry.element.color, index: position)
            )
        }
    }

    /// The slices drawn: with more than `maxSegments`, the smallest fold
    /// into one neutral Other slice. Slices of 0 are left out.
    static func slices(_ ordered: [Slice], maxSegments: Int, other: String) -> [Slice] {
        let limit = max(minimumSegments, maxSegments)
        var result = ordered
        if ordered.count > limit {
            let rest = ordered[(limit - 1)...].reduce(0) { $0 + $1.value }
            result = Array(ordered.prefix(limit - 1))
            result.append(Slice(key: otherKey, label: other, value: rest, color: .neutral))
        }
        return result.filter { $0.value > 0 }
    }

    /// The sum of every segment.
    static func total(_ ordered: [Slice]) -> Double {
        ordered.reduce(0) { $0 + $1.value }
    }

    /// The status to show: an empty state when the total is 0.
    static func resolvedStatus(_ status: MonarchChartFrameStatus, total: Double) -> MonarchChartFrameStatus {
        status == .ready && !(total > 0) ? .empty : status
    }

    /// The thickness of the ring: given, or 22% of the diameter, kept
    /// between the thinnest ring and a full disc.
    static func thickness(_ given: CGFloat?, size: CGFloat) -> CGFloat {
        min(size / 2, max(minimumThickness, given ?? (size * thicknessRatio).rounded()))
    }

    /// The inner radius as a share of the outer radius.
    static func innerRatio(thickness: CGFloat, size: CGFloat) -> CGFloat {
        guard size > 0 else { return 0 }
        return max(0, (size - 2 * thickness) / size)
    }

    /// The inset on each side of a slice: half the gap, or none for one slice.
    static func angularInset(sliceCount: Int) -> CGFloat {
        sliceCount > 1 ? sliceGap / 2 : 0
    }

    /// A part of the whole as a percentage. A share under the smallest step
    /// is written "<1%" and one just under the whole ">99%", so rounding
    /// never hides a segment.
    static func share(_ part: Double, of total: Double, digits: Int) -> String {
        let digits = max(0, digits)
        func percent(_ ratio: Double) -> String {
            ratio.formatted(.percent.precision(.fractionLength(0...digits)))
        }
        guard total > 0, part > 0 else { return percent(0) }
        let ratio = part / total
        let step = pow(10, -Double(digits + 2))
        if ratio < step { return "<" + percent(step) }
        if ratio < 1 && ratio > 1 - step { return ">" + percent(1 - step) }
        return percent(ratio)
    }

    /// The text of a value: the app's formatting, or the locale's number.
    static func text(_ value: Double, format: ((Double) -> String)?) -> String {
        format?(value) ?? value.formatted()
    }

    /// What a legend entry shows after its label, or `nil`.
    static func legendValue(
        value: String,
        share: String,
        content: MonarchDonutChartLegendContent
    ) -> String? {
        switch content {
        case .both: "\(value) · \(share)"
        case .value: value
        case .percent: share
        case .none: nil
        }
    }

    /// The generated summary: the total, then every segment above 0 with
    /// its value and share.
    static func summary(
        _ ordered: [Slice],
        totalLabel: String,
        format: ((Double) -> String)?,
        digits: Int
    ) -> String {
        let total = total(ordered)
        let parts = ordered.filter { $0.value > 0 }.map {
            "\($0.label) \(text($0.value, format: format)) (\(share($0.value, of: total, digits: digits)))"
        }
        return "\(totalLabel) \(text(total, format: format)). \(parts.joined(separator: ", "))"
    }

    /// The data table: every segment, then the total, with a value and a
    /// share column.
    static func table(
        _ ordered: [Slice],
        segmentTitle: String,
        valueTitle: String,
        shareTitle: String,
        totalLabel: String,
        format: ((Double) -> String)?,
        digits: Int
    ) -> MonarchChartTable {
        let total = total(ordered)
        let values = ordered.map(\.value) + [total]
        return MonarchChartTable(
            categoryTitle: segmentTitle,
            categories: ordered.map(\.label) + [totalLabel],
            series: [
                MonarchChartTableSeries(
                    valueTitle,
                    values: values,
                    formattedValues: values.map { text($0, format: format) }
                ),
                MonarchChartTableSeries(
                    shareTitle,
                    values: values.map { total > 0 ? $0 / total * 100 : 0 },
                    formattedValues: values.map { share($0, of: total, digits: digits) }
                ),
            ],
            valueTitle: valueTitle
        )
    }

    /// The data VoiceOver's chart descriptor plays: one value per segment,
    /// without the total, so the audio graph compares the parts.
    static func descriptorTable(
        _ ordered: [Slice],
        segmentTitle: String,
        valueTitle: String,
        format: ((Double) -> String)?
    ) -> MonarchChartTable {
        MonarchChartTable(
            categoryTitle: segmentTitle,
            categories: ordered.map(\.label),
            series: [
                MonarchChartTableSeries(
                    valueTitle,
                    values: ordered.map(\.value),
                    formattedValues: ordered.map { text($0.value, format: format) }
                )
            ],
            valueTitle: valueTitle
        )
    }
}
