import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchDurationInput``. The field
/// takes its measures from the text field, so the two line up in a form.
enum MonarchDurationInputTokens {
    /// The least height of the field, which is also its touch target.
    static let minHeight: CGFloat = MonarchTextFieldTokens.minHeight
    /// Padding before and after the text.
    static let horizontalPadding: CGFloat = MonarchTextFieldTokens.horizontalPadding
    /// Padding above and below the text.
    static let verticalPadding: CGFloat = MonarchTextFieldTokens.verticalPadding
    /// The gap between the label, the field and the supporting text.
    static let stackSpacing: CGFloat = MonarchTextFieldTokens.stackSpacing
    /// The gap between the field and the stepper.
    static let stepperSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the error icon and the error text.
    static let messageIconSpacing: CGFloat = MonarchTextFieldTokens.messageIconSpacing
    /// The SF Symbol shown before an error message.
    static let errorSymbol = MonarchTextFieldTokens.errorSymbol
    /// Minutes added or removed by Page Up and Page Down on a hardware keyboard.
    static let pageStep = MonarchDurationMath.minutesPerHour
    /// Seconds in a minute, for the system duration formatter.
    static let secondsPerMinute: TimeInterval = 60

    /// Why the text was not saved.
    enum Problem: Equatable {
        /// The text cannot be read.
        case invalid
        /// The text reads, but the duration is outside the bounds.
        case outOfRange
    }

    /// What reading the text does.
    enum Commit: Equatable {
        /// Save this value (`nil` when the text is empty).
        case save(Int?)
        /// Keep the text and show the problem.
        case reject(Problem)
    }

    /// Reads the text: empty saves `nil`, a duration inside the bounds saves
    /// it, anything else is rejected and nothing is saved.
    static func commit(_ parsed: MonarchDurationMath.Parsed, minimum: Int, maximum: Int?) -> Commit {
        switch parsed {
        case .empty: return .save(nil)
        case .invalid: return .reject(.invalid)
        case .minutes(let minutes):
            if MonarchDurationMath.isOutOfRange(minutes, minimum: minimum, maximum: maximum) {
                return .reject(.outOfRange)
            }
            return .save(minutes)
        }
    }

    /// The message of a problem, with the bounds written by `format`.
    static func message(
        _ problem: Problem,
        minimum: Int,
        maximum: Int?,
        labels: MonarchDurationInputLabels,
        format: (Int) -> String
    ) -> String {
        switch problem {
        case .invalid:
            return labels.invalid ?? MonarchDurationInputStrings.invalid
        case .outOfRange:
            if let range = labels.range { return range(minimum, maximum) }
            if let maximum {
                return minimum > 0
                    ? MonarchDurationInputStrings.between(format(minimum), format(maximum))
                    : MonarchDurationInputStrings.atMost(format(maximum))
            }
            return MonarchDurationInputStrings.atLeast(format(minimum))
        }
    }

    /// The error to show: the app's first, then the field's own. Empty strings count as none.
    static func shownError(error: String?, problem: String?) -> String? {
        if let error, !error.isEmpty { return error }
        if let problem, !problem.isEmpty { return problem }
        return nil
    }

    /// What VoiceOver reads after the label and value: the error, spoken as
    /// "Error: …", otherwise the helper text.
    static func accessibilityHint(helper: String?, error: String?, labels: MonarchDurationInputLabels) -> String {
        if let error, !error.isEmpty {
            return labels.errorHint?(error) ?? MonarchDurationInputStrings.error(error)
        }
        return helper ?? ""
    }

    /// The spoken duration in the locale: "1 hour, 30 minutes". Empty when there is no value.
    static func valueText(_ minutes: Int?, locale: Locale, labels: MonarchDurationInputLabels) -> String {
        guard let minutes else { return "" }
        if let valueText = labels.valueText { return valueText(minutes) }
        let formatter = DateComponentsFormatter()
        var calendar = Calendar(identifier: .gregorian)
        calendar.locale = locale
        formatter.calendar = calendar
        formatter.unitsStyle = .full
        formatter.allowedUnits = minutes >= MonarchDurationMath.minutesPerHour ? [.hour, .minute] : [.minute]
        formatter.zeroFormattingBehavior = minutes == 0 ? .default : .dropAll
        return formatter.string(from: TimeInterval(max(0, minutes)) * secondsPerMinute) ?? "\(minutes)"
    }

    /// The border role for a state.
    static func border(hasError: Bool, isEnabled: Bool) -> MonarchColorRole {
        MonarchTextFieldTokens.border(hasError: hasError, isEnabled: isEnabled)
    }

    /// The fill role for a state.
    static func background(isEnabled: Bool) -> MonarchColorRole {
        MonarchTextFieldTokens.background(isEnabled: isEnabled)
    }

    /// The colour of the label and the text.
    static func foreground(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textDefault : \.textDisabled
    }

    /// Whether a step up changes the value.
    static func canIncrement(_ value: Int?, maximum: Int?) -> Bool {
        guard let value, let maximum else { return true }
        return value < maximum
    }

    /// Whether a step down changes the value.
    static func canDecrement(_ value: Int?, minimum: Int) -> Bool {
        guard let value else { return true }
        return value > minimum
    }
}
