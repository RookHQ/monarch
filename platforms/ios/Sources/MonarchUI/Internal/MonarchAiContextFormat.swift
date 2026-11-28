import Foundation

/// The symbols and token counts of AI context, shared by the context chips,
/// the mention context and the model picker.
enum MonarchAiContextFormat {
    /// The SF Symbol of a kind of context.
    static func symbol(_ kind: MonarchAiContextKind) -> String {
        switch kind {
        case .file: "doc"
        case .selection: "text.alignleft"
        case .page: "macwindow"
        case .record: "cylinder"
        case .app: "square.grid.2x2"
        case .person: "person"
        }
    }

    /// A token count in short form: "820", "1.2K", "15K".
    static func formatTokens(_ count: Int, locale: Locale = Locale(identifier: "en")) -> String {
        MonarchFormats.capitalisedCompact(
            max(0, count).formatted(
                .number.notation(.compactName).precision(.fractionLength(0...1)).locale(locale)
            )
        )
    }

    /// "1 token" or "1.2K tokens", from the context chips' strings table.
    static func tokens(_ formatted: String, count: Int) -> String {
        count == 1
            ? MonarchStrings.string("monarch.aiContextChips.tokens.one", table: "MonarchAiContextChips")
            : MonarchStrings.string("monarch.aiContextChips.tokens", table: "MonarchAiContextChips", formatted)
    }
}
