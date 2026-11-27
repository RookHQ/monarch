/// The spoken names of the tones, from the `MonarchTone` strings table.
enum MonarchToneStrings {
    /// The spoken name of a tone, or `nil` for the neutral and accent tones,
    /// which carry emphasis but no meaning to announce.
    static func name(_ tone: MonarchTone) -> String? {
        switch tone {
        case .neutral, .accent: nil
        case .info: MonarchStrings.string("monarch.tone.info", table: "MonarchTone")
        case .success: MonarchStrings.string("monarch.tone.success", table: "MonarchTone")
        case .warning: MonarchStrings.string("monarch.tone.warning", table: "MonarchTone")
        case .danger: MonarchStrings.string("monarch.tone.danger", table: "MonarchTone")
        case .ai: MonarchStrings.string("monarch.tone.ai", table: "MonarchTone")
        }
    }
}
