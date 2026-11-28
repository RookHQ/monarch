/// Component tokens for ``MonarchToggleButtonStyle``. Sizes are the button's.
enum MonarchToggleButtonTokens {
    /// The SF Symbol added to an on button when colour must not be the only cue.
    static let checkedSymbol = "checkmark"

    /// Whether the checkmark is shown: only while on, and only when the
    /// Differentiate Without Colour setting is on.
    static func showsCheckmark(isOn: Bool, differentiateWithoutColor: Bool) -> Bool {
        isOn && differentiateWithoutColor
    }

    /// The colour roles for a state. Pure, so it is tested without a view.
    static func appearance(
        variant: MonarchToggleButtonVariant,
        tone: MonarchTone = .accent,
        isOn: Bool,
        isPressed: Bool,
        isEnabled: Bool
    ) -> MonarchSurfaceAppearance {
        guard isEnabled else {
            return MonarchSurfaceAppearance(
                background: isOn ? \.bgMuted : (variant == .outline ? \.bgSurface : nil),
                foreground: \.textDisabled,
                border: isOn || variant == .outline ? \.borderSubtle : nil
            )
        }
        if isOn { return onAppearance(tone: tone, isPressed: isPressed) }
        switch variant {
        case .ghost:
            return MonarchSurfaceAppearance(
                background: isPressed ? \.bgEmphasis : nil,
                foreground: \.textMuted,
                border: nil
            )
        case .outline:
            return MonarchSurfaceAppearance(
                background: isPressed ? \.bgEmphasis : \.bgSurface,
                foreground: \.textMuted,
                border: \.borderDefault
            )
        }
    }

    /// The roles while on: the tone's tinted fill and strong text, with no border.
    /// The accent and AI tones have a pressed fill of their own; the others
    /// use their subtle fill pressed too.
    static func onAppearance(tone: MonarchTone, isPressed: Bool) -> MonarchSurfaceAppearance {
        let palette = MonarchTonePalette.palette(for: tone)
        let pressed: MonarchColorRole =
            switch tone {
            case .accent: \.accentSubtleHover
            case .ai: \.aiSubtleHover
            case .info: \.infoSubtleHover
            case .success: \.successSubtleHover
            case .warning: \.warningSubtleHover
            case .danger: \.dangerSubtleHover
            case .neutral: \.bgEmphasis
            }
        return MonarchSurfaceAppearance(
            background: isPressed ? pressed : palette.subtle,
            foreground: palette.strong,
            border: nil
        )
    }
}
