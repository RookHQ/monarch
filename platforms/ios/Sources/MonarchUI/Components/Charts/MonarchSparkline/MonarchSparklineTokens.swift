import Foundation
import MonarchTokens

/// Component tokens, geometry and text rules for ``MonarchSparkline``.
enum MonarchSparklineTokens {
    /// The width before Dynamic Type is applied.
    static let width: CGFloat = MonarchTokens.Space.space20
    /// The height before Dynamic Type is applied.
    static let height: CGFloat = MonarchTokens.Space.space6
    /// The room kept around the plot, so the line and dots are not clipped.
    static let padding: CGFloat = MonarchTokens.Space.space0_5
    /// The width of the line before Dynamic Type is applied.
    static let lineWidth: CGFloat = MonarchTokens.Space.px + MonarchTokens.Space.px / 2
    /// The radius of a dot before Dynamic Type is applied.
    static let dotRadius: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between two bars, when the bars are wide enough to have one.
    static let barGap: CGFloat = MonarchTokens.Space.px
    /// The least width of a bar's slot for a gap to be drawn.
    static let barGapThreshold: CGFloat = MonarchTokens.Space.space1
    /// The corner radius of a bar.
    static let barRadius: CGFloat = MonarchTokens.Space.px
    /// The least height of a bar, so a zero is still visible.
    static let minBarHeight: CGFloat = MonarchTokens.Space.px

    /// The line and area roles for a tone. `nil` roles mean the surrounding
    /// foreground style.
    static func styles(tone: MonarchTone?, color: MonarchChartColor? = nil) -> (
        line: MonarchColorRole?, area: MonarchColorRole?
    ) {
        if tone == nil, let color {
            let role = MonarchChartLegendTokens.role(color)
            return (role, role)
        }
        guard let tone else { return (nil, nil) }
        let palette = MonarchTonePalette.palette(for: tone)
        return (palette.strong, palette.subtle)
    }

    /// The latest, lowest and highest value of a series.
    struct Stats: Equatable {
        let last: Double
        let minimum: Double
        let maximum: Double
        /// The number of values that are not gaps.
        let count: Int
    }

    /// A series ready to draw at any size. All geometry is pure, so it is
    /// tested without a view.
    struct Plot: Equatable {
        /// The values, with `nil` for every gap.
        let values: [Double?]
        let variant: MonarchSparklineVariant
        /// The value range drawn, never empty.
        let domain: ClosedRange<Double>
        let isInverted: Bool
        /// `nil` when every value is a gap.
        let stats: Stats?

        /// The corner radius of a bar.
        var barRadius: CGFloat { MonarchSparklineTokens.barRadius }

        /// Cleans the values and fixes the range.
        ///
        /// Values that are not finite become gaps. Bars always include zero.
        /// A range with no height is widened by one on both sides, so a flat
        /// series is drawn in the middle.
        init(values: [Double?], variant: MonarchSparklineVariant, range: ClosedRange<Double>?, isInverted: Bool) {
            let clean = values.map { value in value.flatMap { $0.isFinite ? $0 : nil } }
            let present = clean.compactMap { $0 }
            self.values = clean
            self.variant = variant
            self.isInverted = isInverted
            let stats: Stats? =
                if let last = present.last, let minimum = present.min(), let maximum = present.max() {
                    Stats(last: last, minimum: minimum, maximum: maximum, count: present.count)
                } else {
                    nil
                }
            self.stats = stats
            var lower = range?.lowerBound ?? stats?.minimum ?? 0
            var upper = range?.upperBound ?? stats?.maximum ?? 0
            if variant == .bar {
                lower = min(lower, 0)
                upper = max(upper, 0)
            }
            if lower == upper {
                lower -= 1
                upper += 1
            }
            self.domain = lower...upper
        }

        /// A line needs two points; one bar is already a chart.
        var hasEnoughData: Bool {
            (stats?.count ?? 0) >= (variant == .bar ? 1 : 2)
        }

        /// The vertical position of a value. Larger values are higher, unless inverted.
        func y(_ value: Double, in size: CGSize) -> CGFloat {
            let top = MonarchSparklineTokens.padding
            let bottom = max(size.height - MonarchSparklineTokens.padding, top)
            let share =
                (min(max(value, domain.lowerBound), domain.upperBound) - domain.lowerBound)
                / (domain.upperBound - domain.lowerBound)
            return isInverted ? top + (bottom - top) * share : bottom - (bottom - top) * share
        }

        /// The edge an area is filled down to.
        func baseline(in size: CGSize) -> CGFloat {
            isInverted ? MonarchSparklineTokens.padding : max(size.height - MonarchSparklineTokens.padding, 0)
        }

        /// One point per value, `nil` for a gap, evenly spaced from the leading
        /// edge. SwiftUI mirrors the drawn shape in right-to-left layouts.
        func points(in size: CGSize) -> [CGPoint?] {
            let padding = MonarchSparklineTokens.padding
            let step = values.count > 1 ? (size.width - padding * 2) / CGFloat(values.count - 1) : 0
            return values.enumerated().map { index, value in
                value.map { CGPoint(x: padding + CGFloat(index) * step, y: y($0, in: size)) }
            }
        }

        /// The points split at every gap.
        func runs(in size: CGSize) -> [[CGPoint]] {
            var runs: [[CGPoint]] = []
            var current: [CGPoint] = []
            for point in points(in: size) {
                if let point {
                    current.append(point)
                } else if !current.isEmpty {
                    runs.append(current)
                    current = []
                }
            }
            if !current.isEmpty {
                runs.append(current)
            }
            return runs
        }

        /// Where dots are drawn: every point that stands alone between gaps,
        /// because a line cannot show it, and the latest point when asked.
        func dots(in size: CGSize, includesLast: Bool) -> [CGPoint] {
            let runs = runs(in: size)
            var dots = runs.filter { $0.count == 1 }.compactMap(\.first)
            if includesLast, let last = runs.last?.last, !dots.contains(last) {
                dots.append(last)
            }
            return dots
        }

        /// One rectangle per value that is not a gap, from zero to the value.
        func bars(in size: CGSize) -> [CGRect] {
            guard !values.isEmpty else { return [] }
            let slot = size.width / CGFloat(values.count)
            let gap = slot > MonarchSparklineTokens.barGapThreshold ? MonarchSparklineTokens.barGap : 0
            let zero = y(0, in: size)
            return values.enumerated().compactMap { index, value in
                guard let value else { return nil }
                let end = y(value, in: size)
                let height = max(abs(zero - end), MonarchSparklineTokens.minBarHeight)
                // A bar at zero is still drawn, upwards.
                let top = end <= zero ? zero - height : zero
                return CGRect(
                    x: CGFloat(index) * slot + gap / 2,
                    y: top,
                    width: max(slot - gap, MonarchSparklineTokens.minBarHeight),
                    height: height
                )
            }
        }
    }

    /// A number in a locale, with at most one fraction digit.
    static func number(_ value: Double, locale: Locale) -> String {
        value.formatted(.number.precision(.fractionLength(0...1)).locale(locale))
    }

    /// What VoiceOver reads as the value: the latest, lowest and highest
    /// values, then the name of the tone when it has one.
    static func summary(stats: Stats, tone: MonarchTone?, format: (Double) -> String) -> String {
        let figures = MonarchSparklineStrings.summary(
            last: format(stats.last),
            minimum: format(stats.minimum),
            maximum: format(stats.maximum)
        )
        guard let tone, let name = MonarchToneStrings.name(tone) else { return figures }
        return "\(figures). \(name)"
    }
}
