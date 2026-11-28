/// The colour roles of a button in one state.
struct MonarchButtonAppearance: Equatable {
    /// The fill, or `nil` for no fill.
    let background: MonarchColorRole?
    /// The label and icon colour.
    let foreground: MonarchColorRole
    /// The border, or `nil` for no border.
    let border: MonarchColorRole?

    /// The roles for a variant and state. Pure, so it is tested without a view.
    static func resolve(
        variant: MonarchButtonVariant,
        isPressed: Bool,
        isEnabled: Bool
    ) -> MonarchButtonAppearance {
        guard isEnabled else { return disabled(variant) }
        switch variant {
        case .primary:
            return MonarchButtonAppearance(
                background: isPressed ? \.accentSolidActive : \.accentSolid,
                foreground: \.accentOnSolid,
                border: nil
            )
        case .secondary:
            return MonarchButtonAppearance(
                background: isPressed ? \.bgEmphasis : \.bgSurface,
                foreground: \.textDefault,
                border: \.borderDefault
            )
        case .ghost:
            return MonarchButtonAppearance(
                background: isPressed ? \.bgEmphasis : nil,
                foreground: \.textDefault,
                border: nil
            )
        case .danger:
            return MonarchButtonAppearance(
                background: isPressed ? \.dangerSolidActive : \.dangerSolid,
                foreground: \.dangerOnSolid,
                border: nil
            )
        case .dangerSubtle:
            return MonarchButtonAppearance(
                background: isPressed ? \.dangerSubtleHover : \.dangerSubtle,
                foreground: \.dangerStrong,
                border: \.dangerBorder
            )
        case .success:
            return MonarchButtonAppearance(
                background: isPressed ? \.successSolidActive : \.successSolid,
                foreground: \.successOnSolid,
                border: nil
            )
        case .warning:
            return MonarchButtonAppearance(
                background: isPressed ? \.warningSolidActive : \.warningSolid,
                foreground: \.warningOnSolid,
                border: nil
            )
        }
    }

    /// The roles of a disabled button: the same as the enabled one; the style dims the whole button to half
    /// opacity, as the web does, so a disabled primary reads as a pale red button.
    private static func disabled(_ variant: MonarchButtonVariant) -> MonarchButtonAppearance {
        resolve(variant: variant, isPressed: false, isEnabled: true)
    }
}
