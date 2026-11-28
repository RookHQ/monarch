import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and the rules of ``MonarchMetric``.
enum MonarchMetricTokens {
    /// The gap between the label, the figure and the change when stacked.
    static let stackedSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between the label, the figure and the change when inline.
    static let inlineSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the value and its unit.
    static let unitSpacing: CGFloat = MonarchTokens.Space.space1
    /// The weight of the label and the unit.
    static let labelWeight = MonarchTextStyleSpec.fontWeight(MonarchTokens.FontWeight.medium)
    /// The weight of the value.
    static let valueWeight = MonarchTextStyleSpec.fontWeight(MonarchTokens.FontWeight.semibold)
    /// The dash shown for a missing value. Hidden from VoiceOver.
    static let emptyDash = "—"
    /// The role of the label and the unit.
    static var muted: MonarchColorRole { \.textMuted }

    /// The role of the value: subtle for the empty dash.
    static func valueColor(isEmpty: Bool) -> MonarchColorRole {
        isEmpty ? \.textSubtle : \.textDefault
    }

    /// The type role of the label.
    static func labelStyle(_ size: MonarchMetricSize, layout: MonarchMetricLayout) -> MonarchTextStyle {
        if layout == .inline { return .label }
        return size == .lg ? .label : .caption
    }

    /// The type role of the value.
    static func valueStyle(_ size: MonarchMetricSize, layout: MonarchMetricLayout) -> MonarchTextStyle {
        if layout == .inline { return .label }
        switch size {
        case .sm: return .body
        case .md: return .heading
        case .lg: return .display
        }
    }

    /// The type role of the unit.
    static func unitStyle(_ size: MonarchMetricSize, layout: MonarchMetricLayout) -> MonarchTextStyle {
        if layout == .inline { return .caption }
        switch size {
        case .sm: return .caption
        case .md: return .label
        case .lg: return .subheading
        }
    }

    /// The size of the change: one step up for a headline figure.
    static func changeSize(_ size: MonarchMetricSize) -> MonarchDeltaIndicatorSize {
        size == .lg ? .md : .sm
    }

    /// The text alignment that goes with a stack alignment.
    static func textAlignment(_ alignment: HorizontalAlignment) -> TextAlignment {
        switch alignment {
        case .trailing: .trailing
        case .center: .center
        default: .leading
        }
    }

    /// The frame alignment that goes with a stack alignment.
    static func frameAlignment(_ alignment: HorizontalAlignment) -> Alignment {
        switch alignment {
        case .trailing: .trailing
        case .center: .center
        default: .leading
        }
    }

    /// Writes a number the way the metric shows it. `isCompact` writes 1.2K
    /// (for a currency, from iOS 18).
    static func format(_ value: Double, format: MonarchMetricFormat, isCompact: Bool, locale: Locale) -> String {
        switch format {
        case .number:
            let style = FloatingPointFormatStyle<Double>.number.locale(locale)
            return isCompact
                ? MonarchFormats.capitalisedCompact(value.formatted(style.notation(.compactName)))
                : value.formatted(style)
        case .percent:
            let style = FloatingPointFormatStyle<Double>.Percent.percent.locale(locale)
            return isCompact ? value.formatted(style.notation(.compactName)) : value.formatted(style)
        case .currency(let code):
            let style = FloatingPointFormatStyle<Double>.Currency.currency(code: code).locale(locale)
            // The compact notation of currency styles is new in iOS 18; earlier
            // systems write the full amount.
            if isCompact, #available(iOS 18, macOS 15, *) {
                return MonarchFormats.capitalisedCompact(value.formatted(style.notation(.compactName)))
            }
            return value.formatted(style)
        }
    }

    /// What the metric shows and speaks for its value.
    struct Figure: Equatable {
        /// The visible text: the formatted value, or the dash.
        let text: String
        /// What VoiceOver reads: the full value, or the empty label.
        let spoken: String
        /// Whether there is no value.
        let isEmpty: Bool
    }

    /// The figure for a number. A compact figure speaks the full value.
    static func figure(
        _ value: Double?,
        format: MonarchMetricFormat,
        isCompact: Bool,
        emptyLabel: String?,
        locale: Locale
    ) -> Figure {
        guard let value, value.isFinite else { return emptyFigure(emptyLabel) }
        let text = Self.format(value, format: format, isCompact: isCompact, locale: locale)
        let full = isCompact ? Self.format(value, format: format, isCompact: false, locale: locale) : text
        return Figure(text: text, spoken: full, isEmpty: false)
    }

    /// The figure for text the app has already written.
    static func figure(text: String?, spoken: String?, emptyLabel: String?) -> Figure {
        guard let text, !text.isEmpty else { return emptyFigure(emptyLabel) }
        return Figure(text: text, spoken: spoken ?? text, isEmpty: false)
    }

    private static func emptyFigure(_ emptyLabel: String?) -> Figure {
        Figure(text: emptyDash, spoken: emptyLabel ?? MonarchMetricStrings.empty, isEmpty: true)
    }

    /// The sentence the change reads, such as "up 12% vs last week, better".
    static func changeSentence(_ change: MonarchMetricChange, locale: Locale) -> String {
        MonarchDeltaIndicatorTokens.resolve(
            value: change.value,
            comparison: change.comparison,
            goodDirection: change.goodDirection,
            flatThreshold: change.flatThreshold,
            words: MonarchDeltaIndicatorTokens.Words(),
            format: change.formatValue ?? { MonarchDeltaIndicatorTokens.percent($0, locale: locale) }
        ).label
    }

    /// What VoiceOver reads after the label: the value, the unit (not for an
    /// empty value) and the change, separated by commas.
    static func accessibilityValue(figure: Figure, unit: String?, changeSentence: String?) -> String {
        var parts = [figure.spoken]
        if !figure.isEmpty, let unit, !unit.isEmpty {
            parts[0] += " \(unit)"
        }
        if let changeSentence, !changeSentence.isEmpty {
            parts.append(changeSentence)
        }
        return parts.joined(separator: ", ")
    }
}
