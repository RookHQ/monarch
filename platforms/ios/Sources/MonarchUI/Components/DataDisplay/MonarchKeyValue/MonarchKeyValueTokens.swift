import MonarchTokens
import SwiftUI

/// Component tokens and rules for ``MonarchKeyValue``.
enum MonarchKeyValueTokens {
    /// The gap between a separator and the pairs on each side of it.
    static let separatorSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// The gap between rows of wrapped pairs, and between stacked pairs.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the label, the value and the copy button.
    static let pairSpacing: CGFloat = MonarchTokens.Space.space1
    /// The diameter of the dot separator before Dynamic Type is applied.
    static let dotSize: CGFloat = MonarchTokens.Space.space1
    /// The height of the divider separator before Dynamic Type is applied.
    static let dividerHeight: CGFloat = MonarchTokens.Space.space3
    /// The width of the divider separator.
    static let dividerWidth: CGFloat = MonarchShapes.borderWidth
    /// What an empty value shows. Decorative; VoiceOver reads "Not set".
    static let emptyGlyph = "—"
    /// The label colour role.
    static var label: MonarchColorRole { \.textMuted }
    /// The value colour role.
    static var value: MonarchColorRole { \.textDefault }
    /// The colour role of an empty value's dash.
    static var empty: MonarchColorRole { \.textSubtle }

    /// The colour role of a separator, or `nil` when nothing is drawn.
    static func separator(_ separator: MonarchKeyValueSeparator) -> MonarchColorRole? {
        switch separator {
        case .dot: \.textSubtle
        case .divider: \.borderDefault
        case .none: nil
        }
    }

    /// The type role of a size.
    static func textStyle(_ size: MonarchKeyValueSize) -> MonarchTextStyle {
        switch size {
        case .sm: .caption
        case .md: .body
        }
    }

    /// The text style of a value: the size's role, or code for monospaced values.
    static func valueStyle(_ size: MonarchKeyValueSize, isMonospaced: Bool) -> MonarchTextStyle {
        isMonospaced ? .code : textStyle(size)
    }

    /// Whether a value counts as empty.
    static func isEmpty(_ value: String?) -> Bool {
        value?.isEmpty ?? true
    }

    /// What a copy button copies, or `nil` for no button: nothing for an
    /// empty value or an empty copy text.
    static func copyText(_ item: MonarchKeyValueItem) -> String? {
        guard !isEmpty(item.value), let text = item.copyValue, !text.isEmpty else { return nil }
        return text
    }

    /// Whether a separator is drawn before the pair at `index`: in the wrap
    /// layout, before every pair but the first.
    static func showsSeparator(at index: Int, separator: MonarchKeyValueSeparator, layout: MonarchKeyValueLayout)
        -> Bool
    {
        layout == .wrap && separator != .none && index > 0
    }

    /// What VoiceOver reads as the value of a pair.
    static func accessibilityValue(_ value: String?, emptyLabel: String?) -> String {
        if let value, !value.isEmpty { return value }
        return emptyLabel ?? MonarchKeyValueStrings.empty
    }
}
