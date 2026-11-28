import SwiftUI

extension Text {
    /// A number in fixed-width digits, or a dash when there is no number.
    ///
    /// ```swift
    /// Text(monarchTabular: invoice.total, format: .number.precision(.fractionLength(2)))
    /// Text(monarchTabular: share, format: .percent)
    /// ```
    ///
    /// The number is formatted with the given format style, in the user's
    /// locale unless the style names one. A value that is `nil`, or a
    /// floating-point number that is not finite, is shown as a dash that
    /// VoiceOver reads as "No value". The result is a `Text`, so it takes a
    /// type role, a colour and a place in other text like any other.
    ///
    /// - Parameters:
    ///   - value: The number, or `nil` when there is none.
    ///   - format: How the number is written, such as `.number`, `.percent`
    ///     or `.currency(code:)`.
    ///   - emptyText: Shown in place of a missing number. Built in: "—".
    ///   - emptyLabel: Read by VoiceOver in place of a missing number.
    ///     Built in: "No value".
    public init<F: FormatStyle>(
        monarchTabular value: F.FormatInput?,
        format: F,
        emptyText: String? = nil,
        emptyLabel: String? = nil
    ) where F.FormatOutput == String {
        if let formatted = MonarchTabularNumberTokens.formatted(value, format: format) {
            self = Text(verbatim: formatted).monospacedDigit()
        } else {
            self = Text(verbatim: MonarchTabularNumberTokens.emptyText(emptyText))
                .accessibilityLabel(Text(verbatim: MonarchTabularNumberTokens.emptyLabel(emptyLabel)))
        }
    }
}
