/// The colour roles and the icon of one tone. Every toned component (badge,
/// tag, banner, toast, progress, callout, status dot) takes its colours from
/// here, so a tone looks the same everywhere.
struct MonarchTonePalette: Equatable {
    /// A tinted background.
    let subtle: MonarchColorRole
    /// Text and icons on `subtle` or on the canvas.
    let strong: MonarchColorRole
    /// A filled background.
    let solid: MonarchColorRole
    /// Text and icons on `solid`.
    let onSolid: MonarchColorRole
    /// A border around `subtle` or around an outline.
    let border: MonarchColorRole
    /// The SF Symbol that stands for the tone.
    let symbol: String

    /// The palette of a tone.
    static func palette(for tone: MonarchTone) -> MonarchTonePalette {
        switch tone {
        case .neutral:
            MonarchTonePalette(
                subtle: \.bgMuted,
                strong: \.textMuted,
                solid: \.bgInverse,
                onSolid: \.textInverse,
                border: \.borderDefault,
                symbol: "text.bubble"
            )
        case .accent:
            MonarchTonePalette(
                subtle: \.accentSubtle,
                strong: \.accentStrong,
                solid: \.accentSolid,
                onSolid: \.accentOnSolid,
                border: \.accentBorder,
                symbol: "star.circle"
            )
        case .info:
            MonarchTonePalette(
                subtle: \.infoSubtle,
                strong: \.infoStrong,
                solid: \.infoSolid,
                onSolid: \.infoOnSolid,
                border: \.infoBorder,
                symbol: "info.circle"
            )
        case .success:
            MonarchTonePalette(
                subtle: \.successSubtle,
                strong: \.successStrong,
                solid: \.successSolid,
                onSolid: \.successOnSolid,
                border: \.successBorder,
                symbol: "checkmark.circle"
            )
        case .warning:
            MonarchTonePalette(
                subtle: \.warningSubtle,
                strong: \.warningStrong,
                solid: \.warningSolid,
                onSolid: \.warningOnSolid,
                border: \.warningBorder,
                symbol: "exclamationmark.triangle"
            )
        case .danger:
            MonarchTonePalette(
                subtle: \.dangerSubtle,
                strong: \.dangerStrong,
                solid: \.dangerSolid,
                onSolid: \.dangerOnSolid,
                border: \.dangerBorder,
                symbol: "xmark.circle"
            )
        case .ai:
            MonarchTonePalette(
                subtle: \.aiSubtle,
                strong: \.aiStrong,
                solid: \.aiSolid,
                onSolid: \.aiOnSolid,
                border: \.aiBorder,
                symbol: "cpu"
            )
        }
    }

    /// The surface colours of a tone at an emphasis.
    static func surface(tone: MonarchTone, emphasis: MonarchToneEmphasis) -> MonarchSurfaceAppearance {
        let palette = palette(for: tone)
        return switch emphasis {
        case .subtle:
            MonarchSurfaceAppearance(background: palette.subtle, foreground: palette.strong, border: nil)
        case .solid:
            MonarchSurfaceAppearance(background: palette.solid, foreground: palette.onSolid, border: nil)
        case .outline:
            MonarchSurfaceAppearance(background: nil, foreground: palette.strong, border: palette.border)
        }
    }
}
