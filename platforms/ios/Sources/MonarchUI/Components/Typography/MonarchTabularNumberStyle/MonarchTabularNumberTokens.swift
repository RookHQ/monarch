import Foundation

/// Pure logic for ``MonarchTabularNumberStyle`` and
/// `Text(monarchTabular:format:emptyText:emptyLabel:)`.
enum MonarchTabularNumberTokens {
    /// Whether a value has nothing to show: it is `nil`, or a floating-point
    /// or decimal number that is not finite.
    static func isMissing<Value>(_ value: Value?) -> Bool {
        guard let value else { return true }
        if let floating = value as? any BinaryFloatingPoint {
            return !floating.isFinite
        }
        if let decimal = value as? Decimal {
            return !decimal.isFinite
        }
        return false
    }

    /// The formatted number, or `nil` when the value is missing.
    static func formatted<F: FormatStyle>(_ value: F.FormatInput?, format: F) -> String?
    where F.FormatOutput == String {
        guard let value, !isMissing(value) else { return nil }
        return format.format(value)
    }

    /// The text shown in place of a missing number.
    static func emptyText(_ override: String?) -> String {
        override ?? MonarchTabularNumberStrings.empty
    }

    /// What VoiceOver reads in place of a missing number.
    static func emptyLabel(_ override: String?) -> String {
        override ?? MonarchTabularNumberStrings.emptyLabel
    }
}
