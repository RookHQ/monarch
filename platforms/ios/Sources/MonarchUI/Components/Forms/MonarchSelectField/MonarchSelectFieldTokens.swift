import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchSelectField``.
enum MonarchSelectFieldTokens {
    /// The gap between the label, the field and the supporting text.
    static let stackSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// The gap between the value and the chevron.
    static let chevronSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the value and the chevron of the compact style.
    static let compactSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// Padding at the sides of the compact trigger.
    static let compactHorizontalPadding: CGFloat = MonarchTokens.Space.space3
    /// The visible height of the compact trigger: a button's, 36 points.
    static let compactHeight: CGFloat = MonarchTokens.Control.md
    /// How many lines the shown text may take: one, and up to three at the
    /// accessibility text sizes, where a one-line value would be cut.
    static func lineLimit(isAccessibilitySize: Bool) -> Int {
        isAccessibilitySize ? 3 : 1
    }

    /// The SF Symbol of the chevron.
    static let chevronSymbol = "chevron.down"

    /// The colour of the shown text: the value, or the prompt while nothing is chosen.
    static func textColor(hasValue: Bool, isEnabled: Bool) -> MonarchColorRole {
        guard isEnabled else { return \.textDisabled }
        return hasValue ? \.textDefault : \.textSubtle
    }

    /// The colour of the chevron.
    static func chevronColor(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textMuted : \.textDisabled
    }

    /// The text of the field: the title of the selected option, else the prompt, else empty.
    static func shownText<Value: Hashable>(
        selection: Value,
        options: [MonarchSelectOption<Value>],
        prompt: String?
    ) -> (text: String, hasValue: Bool) {
        if let option = options.first(where: { $0.value == selection }) {
            return (option.title, true)
        }
        return (prompt ?? "", false)
    }
}
