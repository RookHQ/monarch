import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchNumberInput``. The field takes its measures
/// from the text field, so the two line up in a form.
enum MonarchNumberInputTokens {
    /// The least height of the field, which is also its touch target.
    static let minHeight: CGFloat = MonarchTextFieldTokens.minHeight
    /// Padding before and after the number.
    static let horizontalPadding: CGFloat = MonarchTextFieldTokens.horizontalPadding
    /// Padding above and below the number.
    static let verticalPadding: CGFloat = MonarchTextFieldTokens.verticalPadding
    /// The gap between the label, the field and the supporting text.
    static let stackSpacing: CGFloat = MonarchTextFieldTokens.stackSpacing
    /// The gap between the text and the − and + buttons inside the box.
    static let stepperSpacing: CGFloat = MonarchTokens.Space.space2
    /// The hairline before each step button.
    static var stepDividerColor: MonarchColorRole { \.borderSubtle }
    /// The colour of a step button's glyph.
    static func stepIconColor(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textMuted : \.textDisabled
    }
    /// The SF Symbol of the button that steps down.
    static let decrementSymbol = "minus"
    /// The SF Symbol of the button that steps up.
    static let incrementSymbol = "plus"

    /// The gap between the error icon and the error text.
    static let messageIconSpacing: CGFloat = MonarchTextFieldTokens.messageIconSpacing
    /// The SF Symbol shown before an error message.
    static let errorSymbol = MonarchTextFieldTokens.errorSymbol
    /// The fewest fraction digits the default format keeps available.
    static let defaultFractionDigits = 3

    /// The keyboards the field asks for.
    enum Keyboard: Equatable {
        /// Digits and the decimal separator.
        case decimal
        /// Digits, the decimal separator and a minus sign.
        case signedDecimal
    }

    /// The border role for a state.
    static func border(hasError: Bool, isEnabled: Bool) -> MonarchColorRole {
        MonarchTextFieldTokens.border(hasError: hasError, isEnabled: isEnabled)
    }

    /// The fill role for a state.
    static func background(isEnabled: Bool) -> MonarchColorRole {
        MonarchTextFieldTokens.background(isEnabled: isEnabled)
    }

    /// The colour of the label and the number.
    static func foreground(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textDefault : \.textDisabled
    }

    /// The keyboard: without a minus sign only when negative numbers are ruled out.
    static func keyboard(minimum: Double?) -> Keyboard {
        if let minimum, minimum >= 0 { return .decimal }
        return .signedDecimal
    }

    /// The most fraction digits of the default format: three, or as many as the step has.
    static func fractionDigits(step: Double) -> Int {
        max(defaultFractionDigits, MonarchNumberInputMath.decimalPlaces(step))
    }

    /// The message for a value outside the bounds, or `nil` when it is inside.
    static func rangeMessage(
        value: Double?,
        minimum: Double?,
        maximum: Double?,
        override: String?,
        format: (Double) -> String
    ) -> String? {
        guard MonarchNumberInputMath.isOutOfRange(value, minimum: minimum, maximum: maximum) else { return nil }
        if let override, !override.isEmpty { return override }
        switch (minimum, maximum) {
        case (let minimum?, let maximum?):
            return MonarchNumberInputStrings.range(minimum: format(minimum), maximum: format(maximum))
        case (let minimum?, nil):
            return MonarchNumberInputStrings.atLeast(format(minimum))
        case (nil, let maximum?):
            return MonarchNumberInputStrings.atMost(format(maximum))
        case (nil, nil):
            return nil
        }
    }

    /// The error to show: the app's first, then the range message. Empty
    /// strings count as none. While the person is typing the range message
    /// waits: it shows once they leave the field.
    static func shownError(error: String?, rangeMessage: String?, isEditing: Bool = false) -> String? {
        if let error, !error.isEmpty { return error }
        if !isEditing, let rangeMessage, !rangeMessage.isEmpty { return rangeMessage }
        return nil
    }

    /// What an edit writes to the value.
    enum Commit: Equatable {
        /// Leave the value as it is: the text is not a number yet.
        case none
        /// Write this value; `nil` for an empty field.
        case value(Double?)
    }

    /// What the text in the field writes to the value, on every edit and
    /// when the person leaves the field: an empty field is `nil`, a number
    /// is written at once (inside the bounds or not, so what the app reads
    /// is what is shown), and text that is not a number changes nothing.
    static func commit(_ text: String, parse: (String) -> Double?) -> Commit {
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty { return .value(nil) }
        guard let number = parse(trimmed), number.isFinite else { return .none }
        return .value(number)
    }

    /// What VoiceOver reads after the label and value: the error, spoken as
    /// "Error: …", otherwise the helper text.
    static func accessibilityHint(helper: String?, error: String?) -> String {
        if let error, !error.isEmpty { return MonarchNumberInputStrings.error(error) }
        return helper ?? ""
    }

    /// What VoiceOver reads as the value of the stepper: the formatted number, or nothing when empty.
    static func accessibilityValue(_ value: Double?, format: (Double) -> String) -> String {
        value.map(format) ?? ""
    }
}
