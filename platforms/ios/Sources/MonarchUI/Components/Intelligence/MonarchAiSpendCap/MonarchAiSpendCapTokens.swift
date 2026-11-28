import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic of ``MonarchAiSpendCap``.
enum MonarchAiSpendCapTokens {
    /// Padding inside the card.
    static let padding: CGFloat = MonarchTokens.Space.space4
    /// The gap between the parts of the card.
    static let spacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between small items.
    static let itemSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between breakdown lines.
    static let lineSpacing: CGFloat = MonarchTokens.Space.space1
    /// The width of the card's border.
    static let borderWidth: CGFloat = MonarchTokens.Space.px
    /// The warn-at share when none is given.
    static let defaultWarnAt = 0.8
    /// The warn-at choices when none are given.
    static let defaultWarnAtOptions = [0.5, 0.75, 0.8, 0.9]
    /// The digits after the decimal point of an amount in micros.
    static let microsDigits = 6
    /// The symbol of the edit button.
    static let editSymbol = "pencil"
    /// The symbol of the read-only note.
    static let lockSymbol = "lock"
    /// The card fill.
    static var fill: MonarchColorRole { \.bgSurface }
    /// The card border.
    static var border: MonarchColorRole { \.borderSubtle }

    // MARK: State

    /// The state of a cap: no limit, reached at or over the limit (a limit of
    /// zero or less is always reached), warning at or above `warnAt` of the
    /// limit, otherwise ok.
    static func state(spent: Int, limit: Int?, warnAt: Double) -> MonarchAiSpendCapState {
        guard let limit else { return .noLimit }
        let used = max(0, spent)
        if limit <= 0 || used >= limit { return .reached }
        if Double(used) >= Double(limit) * warnAt || Double(used) / Double(limit) >= warnAt { return .warning }
        return .ok
    }

    /// The tone and symbol of a state, so it is never told by colour alone.
    static func look(_ state: MonarchAiSpendCapState) -> (tone: MonarchTone, symbol: String) {
        switch state {
        case .ok: (.neutral, "checkmark.circle")
        case .warning: (.warning, "exclamationmark.triangle")
        case .reached: (.danger, "xmark.circle")
        case .noLimit: (.neutral, "info.circle")
        }
    }

    /// The tone of the bar: neutral within the limit, warning when close, danger when reached.
    static func barTone(_ state: MonarchAiSpendCapState) -> MonarchTone {
        switch state {
        case .ok, .noLimit: .neutral
        case .warning: .warning
        case .reached: .danger
        }
    }

    /// The share of the limit spent, limited to 0 through 1.
    static func fraction(spent: Int, limit: Int) -> Double {
        guard limit > 0 else { return 1 }
        return min(1, max(0, Double(spent) / Double(limit)))
    }

    /// Whether AI is paused: the limit is reached and the behaviour is pause.
    static func isPaused(_ state: MonarchAiSpendCapState, behavior: MonarchAiSpendCapBehavior) -> Bool {
        state == .reached && behavior == .pause
    }

    // MARK: Amounts

    /// The digits of the unit: the currency's minor digits, or six for micros.
    static func unitDigits(currencyCode: String, unit: MonarchAiSpendCapUnit) -> Int {
        unit == .micros ? microsDigits : MonarchMoneyTokens.fractionDigits(currencyCode: currencyCode)
    }

    /// An integer amount as an exact decimal amount of major units: 1240 minor GBP is 12.40.
    static func decimal(_ amount: Int, currencyCode: String, unit: MonarchAiSpendCapUnit) -> Decimal {
        Decimal(
            sign: amount < 0 ? .minus : .plus,
            exponent: -unitDigits(currencyCode: currencyCode, unit: unit),
            significand: Decimal(amount.magnitude)
        )
    }

    /// An integer amount as money, without a floating-point step.
    static func money(_ amount: Int, currencyCode: String, unit: MonarchAiSpendCapUnit, locale: Locale) -> String {
        MonarchMoney.formatted(
            decimal(amount, currencyCode: currencyCode, unit: unit),
            currencyCode: currencyCode,
            locale: locale
        )
    }

    /// An integer amount as major units for the amount field: 1240 minor GBP is 12.4.
    static func toMajor(_ amount: Int, currencyCode: String, unit: MonarchAiSpendCapUnit) -> Double {
        let digits = MonarchMoneyTokens.fractionDigits(currencyCode: currencyCode)
        var value = decimal(amount, currencyCode: currencyCode, unit: unit)
        var rounded = Decimal()
        NSDecimalRound(&rounded, &value, digits, .plain)
        return NSDecimalNumber(decimal: rounded).doubleValue
    }

    /// A typed major amount as an integer in the unit, rounded to the
    /// currency's own digits first: 12.4 GBP is 1240 minor or 12 400 000 micros.
    static func fromMajor(_ major: Double, currencyCode: String, unit: MonarchAiSpendCapUnit) -> Int {
        let digits = MonarchMoneyTokens.fractionDigits(currencyCode: currencyCode)
        var value = Decimal(string: String(major), locale: Locale(identifier: "en_US_POSIX")) ?? Decimal(major)
        var rounded = Decimal()
        NSDecimalRound(&rounded, &value, digits, .plain)
        let shifted = rounded * pow(Decimal(10), unitDigits(currencyCode: currencyCode, unit: unit))
        return NSDecimalNumber(decimal: shifted).intValue
    }

    /// A share as a whole percentage: 0.8 is "80%".
    static func percent(_ share: Double, locale: Locale) -> String {
        share.formatted(.percent.precision(.fractionLength(0)).locale(locale))
    }

    // MARK: Warn at

    /// The warn-at choices: the options and the current value, between 0 and 1, sorted.
    static func warnAtShares(options: [Double], current: Double) -> [Double] {
        Array(Set(options + [current])).filter { $0 > 0 && $0 < 1 }.sorted()
    }

    /// The warn-at value the form starts with: the current one, or the nearest choice.
    static func initialWarnAt(shares: [Double], current: Double) -> Double {
        guard !shares.isEmpty, !shares.contains(current) else { return current }
        return shares.min { abs($0 - current) < abs($1 - current) } ?? current
    }

    // MARK: Validation

    /// What happens when the person saves the form.
    enum Validation: Equatable {
        /// The amount is empty.
        case required
        /// The amount is zero or less.
        case notPositive
        /// The amount is above the highest limit.
        case aboveMaximum
        /// The amount is below what is spent; saving the same amount again confirms.
        case belowSpent(Int)
        /// The limit to save, as an integer in the unit.
        case valid(Int)
    }

    /// Checks a typed amount. `warnedBelow` is the below-spent amount the
    /// person was already warned about; saving it again is valid.
    static func validate(
        major: Double?,
        spent: Int,
        maxLimit: Int?,
        warnedBelow: Int?,
        currencyCode: String,
        unit: MonarchAiSpendCapUnit
    ) -> Validation {
        guard let major else { return .required }
        let limit = fromMajor(major, currencyCode: currencyCode, unit: unit)
        guard limit > 0 else { return .notPositive }
        if let maxLimit, limit > maxLimit { return .aboveMaximum }
        if limit < max(0, spent), warnedBelow != limit { return .belowSpent(limit) }
        return .valid(limit)
    }

    // MARK: Text

    /// The phrases of one cap: money, period and labels.
    struct Phrases {
        let currencyCode: String
        let unit: MonarchAiSpendCapUnit
        let period: MonarchAiSpendCapPeriod
        let locale: Locale
        let labels: MonarchAiSpendCapLabels

        func money(_ amount: Int) -> String {
            MonarchAiSpendCapTokens.money(amount, currencyCode: currencyCode, unit: unit, locale: locale)
        }

        var periodText: String { labels.period?(period) ?? MonarchAiSpendCapStrings.period(period) }

        var title: String { labels.title ?? MonarchAiSpendCapStrings.title }

        func state(_ state: MonarchAiSpendCapState) -> String {
            labels.state?(state) ?? MonarchAiSpendCapStrings.state(state)
        }

        /// "£12.40 of £50.00 this month" or "£12.40 spent this month".
        func amountLine(spent: Int, limit: Int?) -> String {
            let used = money(max(0, spent))
            guard let limit else {
                return labels.spentNoLimit?(used, periodText) ?? MonarchAiSpendCapStrings.spentNoLimit(used, periodText)
            }
            let cap = money(limit)
            return labels.spentOfLimit?(used, cap, periodText)
                ?? MonarchAiSpendCapStrings.spentOfLimit(used, cap, periodText)
        }

        /// What is left, or how far spending is over.
        func remainder(spent: Int, limit: Int) -> String {
            let used = max(0, spent)
            if used > limit {
                let amount = money(used - limit)
                return labels.over?(amount) ?? MonarchAiSpendCapStrings.over(amount)
            }
            let amount = money(limit - used)
            return labels.remaining?(amount) ?? MonarchAiSpendCapStrings.remaining(amount)
        }

        func warnsAt(_ share: Double) -> String {
            let text = percent(share, locale: locale)
            return labels.warnsAt?(text) ?? MonarchAiSpendCapStrings.warnsAt(text)
        }

        func warnAtOption(_ share: Double) -> String {
            let text = percent(share, locale: locale)
            return labels.warnAtOption?(text) ?? MonarchAiSpendCapStrings.warnAtOption(text)
        }

        func behaviorSummary(_ behavior: MonarchAiSpendCapBehavior) -> String {
            labels.behaviorSummary?(behavior) ?? MonarchAiSpendCapStrings.behaviorSummary(behavior)
        }

        func behaviorOption(_ behavior: MonarchAiSpendCapBehavior) -> String {
            labels.behaviorOption?(behavior) ?? MonarchAiSpendCapStrings.behaviorOption(behavior)
        }

        func behaviorDescription(_ behavior: MonarchAiSpendCapBehavior) -> String {
            labels.behaviorDescription?(behavior) ?? MonarchAiSpendCapStrings.behaviorDescription(behavior)
        }

        /// "Resets 1 June", with the time for a daily limit.
        func resets(_ date: Date, timeZone: TimeZone) -> String {
            var style = Date.FormatStyle(date: .omitted, time: .omitted, locale: locale, timeZone: timeZone)
                .day()
                .month(.wide)
            if period == .day { style = style.monarchTime() }
            let text = date.formatted(style)
            return labels.resets?(text) ?? MonarchAiSpendCapStrings.resets(text)
        }

        func pausedBody(limit: Int) -> String {
            let cap = money(limit)
            return labels.pausedBody?(cap, periodText) ?? MonarchAiSpendCapStrings.pausedBody(cap, periodText)
        }

        func amountHint(spent: Int) -> String {
            let used = money(max(0, spent))
            return labels.amountHint?(used, periodText) ?? MonarchAiSpendCapStrings.amountHint(used, periodText)
        }

        /// The message for a validation result, or `nil` when it is valid.
        func message(_ validation: Validation, spent: Int, maxLimit: Int?, behavior: MonarchAiSpendCapBehavior)
            -> String?
        {
            switch validation {
            case .required:
                return labels.errorRequired ?? MonarchAiSpendCapStrings.errorRequired
            case .notPositive:
                return labels.errorPositive ?? MonarchAiSpendCapStrings.errorPositive
            case .aboveMaximum:
                let max = money(maxLimit ?? 0)
                return labels.errorMax?(max) ?? MonarchAiSpendCapStrings.errorMax(max)
            case .belowSpent:
                let used = money(Swift.max(0, spent))
                return labels.belowSpent?(used, periodText, behavior)
                    ?? MonarchAiSpendCapStrings.belowSpent(used, periodText, behavior)
            case .valid:
                return nil
            }
        }

        /// The announcement for a new state.
        func announcement(_ state: MonarchAiSpendCapState, amountLine: String) -> String {
            let name = self.state(state)
            return labels.announce?(title, name, amountLine)
                ?? MonarchAiSpendCapStrings.announce(title, name, amountLine)
        }
    }
}
