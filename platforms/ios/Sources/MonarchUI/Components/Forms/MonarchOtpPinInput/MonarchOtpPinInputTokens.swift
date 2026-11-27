import MonarchTokens
import SwiftUI

/// Component tokens and logic for ``MonarchOtpPinInput``.
enum MonarchOtpPinInputTokens {
    /// The width of a cell before Dynamic Type is applied.
    static func cellWidth(_ size: MonarchOtpPinInputSize) -> CGFloat {
        switch size {
        case .md: MonarchTokens.Space.space10 + MonarchTokens.Space.space1
        case .lg: MonarchTokens.Space.space12 + MonarchTokens.Space.space1
        }
    }

    /// The height of a cell before Dynamic Type is applied. Never under the touch target.
    static func cellHeight(_ size: MonarchOtpPinInputSize) -> CGFloat {
        switch size {
        case .md: max(MonarchTokens.Control.lg, MonarchAccessibility.minimumHitTarget)
        case .lg: max(MonarchTokens.Control.xl, MonarchAccessibility.minimumHitTarget)
        }
    }

    /// The type role of the character in a cell.
    static func textStyle(_ size: MonarchOtpPinInputSize) -> MonarchTextStyle {
        switch size {
        case .md: .subheading
        case .lg: .heading
        }
    }

    /// The gap between cells, and the extra gap between groups.
    static let cellSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the label, the cells and the supporting text.
    static let stackSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// The gap between the error icon and the error text.
    static let messageIconSpacing: CGFloat = MonarchTokens.Space.space1
    /// The corner radius of a cell.
    static let cornerRadius: CGFloat = MonarchTokens.Radius.md
    /// The width of the border of a cell.
    static let borderWidth: CGFloat = MonarchTokens.Space.px
    /// The width of the caret in the active cell.
    static let caretWidth: CGFloat = MonarchTokens.Space.space0_5
    /// The height of the caret as a part of the cell height.
    static let caretHeightRatio: CGFloat = 0.5
    /// The diameter of the dot that stands for a masked character.
    static let dotSize: CGFloat = MonarchTokens.Space.space2_5
    /// How far a character may shrink to stay inside a narrow cell.
    static let minimumScaleFactor: CGFloat = 0.5
    /// The SF Symbol shown before an error message.
    static let errorSymbol = "exclamationmark.circle"

    /// The number of cells for a requested length: at least one.
    static func length(_ requested: Int) -> Int {
        max(1, requested)
    }

    /// The code in `raw`: digits (from any script, stored as ASCII) and, in the
    /// alphanumeric mode, the letters A to Z in upper case, cut to `length`.
    static func clean(_ raw: String, mode: MonarchOtpPinInputMode, length: Int) -> String {
        var result = ""
        for character in raw {
            guard result.count < length else { break }
            if let digit = asciiDigit(character) {
                result.append(digit)
            } else if mode == .alphanumeric, character.isASCII, character.isLetter {
                result.append(contentsOf: character.uppercased())
            }
        }
        return result
    }

    /// The ASCII digit of a decimal digit in any script, such as Arabic-Indic or full-width digits.
    private static func asciiDigit(_ character: Character) -> Character? {
        let scalars = character.unicodeScalars
        guard scalars.count == 1, let scalar = scalars.first,
            scalar.properties.generalCategory == .decimalNumber,
            let value = character.wholeNumberValue, (0...9).contains(value)
        else { return nil }
        return Character(String(value))
    }

    /// What typing does to the code.
    struct Change: Equatable {
        /// The new code.
        let value: String
        /// Whether the new code has every character.
        let isComplete: Bool
    }

    /// The change that typed text makes to the current code, or `nil` when the
    /// cleaned text equals the current code.
    static func change(from current: String, typed: String, mode: MonarchOtpPinInputMode, length: Int) -> Change? {
        let cleaned = clean(typed, mode: mode, length: length)
        guard cleaned != current else { return nil }
        return Change(value: cleaned, isComplete: cleaned.count == length)
    }

    /// The index of the cell that takes the next character, or `nil` without focus.
    static func activeIndex(count: Int, length: Int, isFocused: Bool) -> Int? {
        isFocused ? min(count, length - 1) : nil
    }

    /// Whether a cell shows the caret: the active cell, while it is empty.
    static func showsCaret(at index: Int, count: Int, length: Int, isFocused: Bool) -> Bool {
        count < length && activeIndex(count: count, length: length, isFocused: isFocused) == index
    }

    /// Whether a wider gap comes before a cell, for a code drawn in groups.
    static func hasSeparator(before index: Int, groupSize: Int?) -> Bool {
        guard let groupSize, groupSize > 0, index > 0 else { return false }
        return index.isMultiple(of: groupSize)
    }

    /// The border role of a cell.
    static func border(hasError: Bool, isEnabled: Bool) -> MonarchColorRole {
        if !isEnabled { return \.borderSubtle }
        return hasError ? \.dangerStrong : \.borderInput
    }

    /// The fill role of a cell.
    static func background(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.bgSurface : \.bgSubtle
    }

    /// The role of the character, the dot and the caret.
    static func foreground(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textDefault : \.textDisabled
    }

    /// What VoiceOver reads after the label and value: the length of the code,
    /// then the error if there is one, otherwise the helper text.
    static func accessibilityHint(length: Int, lengthHint: String?, helper: String?, error: String?) -> String {
        var parts = [lengthHint ?? MonarchOtpPinInputStrings.lengthHint(length)]
        if let error, !error.isEmpty {
            parts.append(MonarchOtpPinInputStrings.error(error))
        } else if let helper, !helper.isEmpty {
            parts.append(helper)
        }
        return parts.filter { !$0.isEmpty }.joined(separator: ". ")
    }
}
