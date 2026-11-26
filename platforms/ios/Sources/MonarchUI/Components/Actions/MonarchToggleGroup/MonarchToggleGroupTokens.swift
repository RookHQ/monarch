import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchToggleGroup``. Item sizes
/// and colours are those of ``MonarchToggleButtonStyle``.
enum MonarchToggleGroupTokens {
    /// The gap between two items.
    static let itemSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the symbol, the label and the count of an item.
    static let contentSpacing: CGFloat = MonarchButtonTokens.iconSpacing

    /// The selection of a multiple-choice group after one item is switched.
    static func toggled<Value: Hashable>(_ selection: Set<Value>, value: Value, isOn: Bool) -> Set<Value> {
        var next = selection
        if isOn {
            next.insert(value)
        } else {
            next.remove(value)
        }
        return next
    }

    /// The selection of a single-choice group after one item is switched.
    /// Switching an item on replaces the selection. Switching the selected
    /// item off clears it, unless the group requires a selection.
    static func toggled<Value: Hashable>(_ selection: Value?, value: Value, isOn: Bool, isRequired: Bool) -> Value? {
        if isOn { return value }
        guard selection == value, !isRequired else { return selection }
        return nil
    }

    /// Whether the label text is drawn. An icon-only item without a symbol
    /// still shows its text, so it is never blank.
    static func showsText(isIconOnly: Bool, systemImage: String?) -> Bool {
        !(isIconOnly && systemImage != nil)
    }

    /// What VoiceOver reads for an item: the label, then the count.
    static func accessibilityLabel(label: String, count: String?) -> String {
        guard let count, !count.isEmpty else { return label }
        return "\(label), \(count)"
    }

    /// The role of the count beside the label.
    static func count(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textSubtle : \.textDisabled
    }
}
