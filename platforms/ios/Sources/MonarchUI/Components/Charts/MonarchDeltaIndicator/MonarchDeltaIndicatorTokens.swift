import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and the rules of ``MonarchDeltaIndicator``.
enum MonarchDeltaIndicatorTokens {
    /// The gap between the change and the comparison.
    static let spacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the arrow and the value.
    static let iconSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The weight of the value.
    static let fontWeight = MonarchTextStyleSpec.fontWeight(MonarchTokens.FontWeight.semibold)
    /// The minus sign written before a decrease.
    static let minus = "−"
    /// The plus sign written before an increase.
    static let plus = "+"

    /// Which way the value moved.
    enum Direction: Equatable {
        case up, down, flat
    }

    /// Whether the change is good, bad or neither.
    enum Tone: Equatable {
        case positive, negative, neutral
    }

    /// The built-in words, with the overrides applied.
    struct Words {
        let up: String
        let down: String
        let flat: String
        let better: String
        let worse: String
        let noData: String

        /// The words from the strings table. `flat` and `noData` can be replaced.
        init(flat: String? = nil, noData: String? = nil) {
            self.up = MonarchDeltaIndicatorStrings.up
            self.down = MonarchDeltaIndicatorStrings.down
            self.flat = flat ?? MonarchDeltaIndicatorStrings.flat
            self.better = MonarchDeltaIndicatorStrings.better
            self.worse = MonarchDeltaIndicatorStrings.worse
            self.noData = noData ?? MonarchDeltaIndicatorStrings.noData
        }
    }

    /// What the indicator shows and speaks.
    struct Result: Equatable {
        /// `nil` when there is nothing to compare with.
        let direction: Direction?
        let tone: Tone
        /// The visible text: "+12%", "−3 ms", "No change" or "No earlier data".
        let text: String
        /// The spoken sentence: "up 12% vs last month, better".
        let label: String
    }

    /// Padding before and after the value. Only the pill has any.
    static func horizontalPadding(_ variant: MonarchDeltaIndicatorVariant) -> CGFloat {
        variant == .subtle ? MonarchTokens.Space.space2 : 0
    }

    /// Padding above and below the value. Only the pill has any.
    static func verticalPadding(_ variant: MonarchDeltaIndicatorVariant) -> CGFloat {
        variant == .subtle ? MonarchTokens.Space.space0_5 : 0
    }

    /// The type role for a size.
    static func textStyle(_ size: MonarchDeltaIndicatorSize) -> MonarchTextStyle {
        switch size {
        case .sm: .caption
        case .md: .label
        }
    }

    /// The SF Symbol for a direction, or `nil` when there is nothing to compare with.
    static func symbol(for direction: Direction?) -> String? {
        switch direction {
        case .up: "arrow.up.right"
        case .down: "arrow.down.right"
        case .flat: "minus"
        case nil: nil
        }
    }

    /// The colour roles for a tone. The pill takes the shared tone palette.
    static func appearance(tone: Tone, variant: MonarchDeltaIndicatorVariant) -> MonarchSurfaceAppearance {
        let shared: MonarchTone =
            switch tone {
            case .positive: .success
            case .negative: .danger
            case .neutral: .neutral
            }
        let surface = MonarchTonePalette.surface(tone: shared, emphasis: .subtle)
        switch variant {
        case .subtle: return surface
        case .plain: return MonarchSurfaceAppearance(background: nil, foreground: surface.foreground, border: nil)
        }
    }

    /// The change between two periods as a ratio of the previous one, or
    /// `nil` when there is nothing to compare with.
    static func ratio(current: Double?, previous: Double?) -> Double? {
        guard let current, let previous, previous != 0 else { return nil }
        let ratio = (current - previous) / abs(previous)
        return ratio.isFinite ? ratio : nil
    }

    /// A size as a percentage with at most one fraction digit.
    static func percent(_ magnitude: Double, locale: Locale) -> String {
        magnitude.formatted(.percent.precision(.fractionLength(0...1)).locale(locale))
    }

    /// Works out the direction, tone, text and spoken sentence of a change.
    /// Pure, so it is tested without a view.
    ///
    /// A change whose size is at or below `flatThreshold`, or that `format`
    /// writes the same as zero, is flat: "0%" is "No change", never "+0%".
    static func resolve(
        value: Double?,
        comparison: String?,
        goodDirection: MonarchDeltaIndicatorGoodDirection,
        flatThreshold: Double,
        words: Words,
        format: (Double) -> String
    ) -> Result {
        guard let value, value.isFinite else {
            return Result(direction: nil, tone: .neutral, text: words.noData, label: words.noData)
        }
        let tail = comparison.flatMap { $0.isEmpty ? nil : " \($0)" } ?? ""
        let size = abs(value)
        if size <= flatThreshold || format(size) == format(0) {
            return Result(direction: .flat, tone: .neutral, text: words.flat, label: words.flat + tail)
        }
        let direction: Direction = value > 0 ? .up : .down
        let tone: Tone =
            switch (goodDirection, direction) {
            case (.neither, _): .neutral
            case (.up, .up), (.down, .down): .positive
            default: .negative
            }
        let magnitude = format(size)
        let quality: String =
            switch tone {
            case .positive: ", \(words.better)"
            case .negative: ", \(words.worse)"
            case .neutral: ""
            }
        return Result(
            direction: direction,
            tone: tone,
            text: (direction == .up ? plus : minus) + magnitude,
            label: "\(direction == .up ? words.up : words.down) \(magnitude)\(tail)\(quality)"
        )
    }
}
