import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchCopyField``.
enum MonarchCopyFieldTokens {
    /// The gap between the label and the row.
    static let stackSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// The gap between the box and the button.
    static let buttonSpacing: CGFloat = MonarchTokens.Space.space2
    /// Padding before and after the value.
    static let horizontalPadding: CGFloat = MonarchTokens.Space.space3
    /// Padding above and below the value.
    static let verticalPadding: CGFloat = MonarchTokens.Space.space1_5
    /// The number of lines of a multi-line field, before it grows: unlimited.
    static let multilineLimit: Int? = nil
    /// Shown in the box when the value is empty, so the box keeps its height.
    static let emptyDisplay = " "

    /// The least height of the box: the height of the button beside it.
    static func minHeight(_ size: MonarchButtonSize) -> CGFloat {
        MonarchButtonTokens.minHeight(size)
    }

    /// The type role of the value.
    static func textStyle(isMonospaced: Bool) -> MonarchTextStyle {
        isMonospaced ? .code : .body
    }

    /// The line limit: one line, or the whole value.
    static func lineLimit(isMultiline: Bool) -> Int? {
        isMultiline ? multilineLimit : 1
    }

    /// How the box and the button line up: centred on one line, at the top
    /// when the value wraps.
    static func rowAlignment(isMultiline: Bool) -> VerticalAlignment {
        isMultiline ? .top : .center
    }

    /// The text drawn in the box.
    static func displayText(_ value: String) -> String {
        value.isEmpty ? emptyDisplay : value
    }

    /// What VoiceOver reads after the label: always the whole value.
    static func accessibilityValue(_ value: String) -> String {
        value
    }

    /// The role of the label above the box.
    static func label(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textDefault : \.textDisabled
    }

    /// The role of the value.
    static func text(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textDefault : \.textDisabled
    }

    /// The fill of the box. A read-only value sits on the subtle fill.
    static func background(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.bgSubtle : \.bgMuted
    }

    /// The border of the box.
    static func border(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.borderInput : \.borderSubtle
    }
}
