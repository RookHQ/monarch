/// The tone colours of a card-like surface whose title and border carry a
/// tone: the section card, the stat card. Neutral keeps the card's own
/// border and default text.
enum MonarchToneChrome {
    /// The border: the card border for neutral, the tone's border otherwise.
    static func border(_ tone: MonarchTone, neutral: MonarchColorRole = \.borderSubtle) -> MonarchColorRole {
        tone == .neutral ? neutral : MonarchTonePalette.palette(for: tone).border
    }

    /// The title colour: default text for neutral, the tone's strong role otherwise.
    static func titleColor(_ tone: MonarchTone) -> MonarchColorRole {
        tone == .neutral ? \.textDefault : MonarchTonePalette.palette(for: tone).strong
    }

    /// The SF Symbol before the title: the given one, otherwise the tone's
    /// for every tone but neutral, so the tone does not depend on colour.
    static func symbol(_ tone: MonarchTone, override: String?) -> String? {
        override ?? (tone == .neutral ? nil : MonarchTonePalette.palette(for: tone).symbol)
    }
}
