import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchPeriodSelector``.
enum MonarchPeriodSelectorTokens {
    /// The gap between the control, the custom range and the dates.
    static let spacing: CGFloat = MonarchTokens.Space.space2
    /// The colour of the resolved dates.
    static var rangeColor: MonarchColorRole { \.textMuted }

    /// The shown name of a preset: the app's, or the built-in one.
    static func name(_ preset: MonarchPeriodPreset, labels: MonarchPeriodSelectorLabels) -> String {
        labels.presets[preset] ?? MonarchPeriodSelectorStrings.preset(preset)
    }

    /// The variant drawn: segments only below the accessibility text sizes,
    /// where the segmented control does not grow.
    static func resolvedVariant(_ variant: MonarchPeriodSelectorVariant, isAccessibilitySize: Bool)
        -> MonarchPeriodSelectorVariant
    {
        isAccessibilitySize ? .menu : variant
    }

    /// The period after a preset is picked: the same preset changes nothing,
    /// and Custom starts from the dates already shown, so nothing jumps.
    static func picking(
        _ preset: MonarchPeriodPreset,
        current: MonarchPeriod,
        today: Date,
        calendar: Calendar
    ) -> MonarchPeriod {
        guard preset != current.preset else { return current }
        if preset == .custom {
            return MonarchPeriod(.custom, range: current.range ?? current.resolved(today: today, calendar: calendar))
        }
        return MonarchPeriod(preset, range: current.range)
    }

    /// Whether the resolved dates are shown: asked for, resolved, and not a
    /// custom period (whose pickers already show them).
    static func showsDates(_ period: MonarchPeriod, showsRange: Bool, resolved: ClosedRange<Date>?) -> Bool {
        showsRange && resolved != nil && period.preset != .custom
    }

    /// The dates of a period for display: "Oct 1 – 30, 2026", or one day.
    static func rangeText(_ range: ClosedRange<Date>, locale: Locale, calendar: Calendar) -> String {
        if calendar.isDate(range.lowerBound, inSameDayAs: range.upperBound) {
            return Date.FormatStyle(
                date: .abbreviated,
                time: .omitted,
                locale: locale,
                calendar: calendar,
                timeZone: calendar.timeZone
            )
            .format(range.lowerBound)
        }
        return Date.IntervalFormatStyle(
            date: .abbreviated,
            time: .omitted,
            locale: locale,
            calendar: calendar,
            timeZone: calendar.timeZone
        )
        .format(range.lowerBound..<range.upperBound)
    }
}
