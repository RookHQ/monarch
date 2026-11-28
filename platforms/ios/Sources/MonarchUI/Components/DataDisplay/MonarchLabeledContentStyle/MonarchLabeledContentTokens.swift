import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchLabeledContentStyle``.
enum MonarchLabeledContentTokens {
    /// The gap between the label and the value when stacked.
    static let stackedSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The least gap between the label and the value when inline.
    static let inlineSpacing: CGFloat = MonarchTokens.Space.space4
    /// The type role of the label.
    static let labelStyle: MonarchTextStyle = .caption
    /// The type role of the value.
    static let valueStyle: MonarchTextStyle = .body

    /// The role of the label: muted, so the value reads first.
    static func label(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textMuted : \.textDisabled
    }

    /// The role of the value.
    static func value(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textDefault : \.textDisabled
    }

    /// How the value is aligned in its own lines: with the label when
    /// stacked, against the trailing edge when inline.
    static func valueAlignment(_ layout: MonarchLabeledContentLayout) -> TextAlignment {
        layout == .inline ? .trailing : .leading
    }
}
