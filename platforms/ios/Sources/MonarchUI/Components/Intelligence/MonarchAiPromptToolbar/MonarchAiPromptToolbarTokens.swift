import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchAiPromptToolbar``.
enum MonarchAiPromptToolbarTokens {
    /// The gap between controls and between wrapped rows.
    static let spacing: CGFloat = MonarchTokens.Space.space1
    /// The button size of every control in the row.
    static let controlSize: MonarchButtonSize = .sm
    /// The variant of plain buttons in the row: quiet, so the prompt stays the focus.
    static let buttonVariant: MonarchButtonVariant = .ghost
    /// The symbol added to a toggle that is on when the person asks for
    /// differentiation without colour.
    static let checkedSymbol = "checkmark"

    /// Whether a toggle shows the check mark.
    static func showsCheckmark(isOn: Bool, differentiateWithoutColor: Bool) -> Bool {
        isOn && differentiateWithoutColor
    }

    /// The roles of a toggle in a state. On is the assistant subtle fill with
    /// the assistant border and strong text, so it differs from off by more
    /// than colour (off has no border).
    static func toggleAppearance(isOn: Bool, isPressed: Bool, isEnabled: Bool) -> MonarchSurfaceAppearance {
        guard isEnabled else {
            return MonarchSurfaceAppearance(
                background: isOn ? \.bgMuted : nil,
                foreground: \.textDisabled,
                border: isOn ? \.borderSubtle : nil
            )
        }
        if isOn {
            return MonarchSurfaceAppearance(
                background: isPressed ? \.aiSubtleHover : \.aiSubtle,
                foreground: \.aiStrong,
                border: \.aiBorder
            )
        }
        return MonarchSurfaceAppearance(
            background: isPressed ? \.bgEmphasis : nil,
            foreground: \.textMuted,
            border: nil
        )
    }
}
