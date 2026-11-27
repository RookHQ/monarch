import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchSkipLink``.
enum MonarchSkipLinkTokens {
    /// The symbol before the title.
    static let symbol = "arrow.down.to.line"
    /// The variant of the button.
    static let variant: MonarchButtonVariant = .secondary
    /// The size of the button.
    static let size: MonarchButtonSize = .sm

    /// Whether the control is drawn: always when asked, otherwise while VoiceOver or Switch Control runs or while
    /// it has keyboard focus, as a web skip link appears when it is focused.
    static func isVisible(
        isAlwaysVisible: Bool,
        isVoiceOverRunning: Bool,
        isSwitchControlRunning: Bool,
        isFocused: Bool
    ) -> Bool {
        isAlwaysVisible || isVoiceOverRunning || isSwitchControlRunning || isFocused
    }
}
