/// Built-in strings of the AI tool result card, from the `MonarchAiToolResultCard` strings table.
enum MonarchAiToolResultCardStrings {
    private static let table = "MonarchAiToolResultCard"

    /// The label above the title for a kind.
    static func kind(_ kind: MonarchAiToolResultKind) -> String {
        switch kind {
        case .recordCreated: MonarchStrings.string("monarch.aiToolResultCard.kind.recordCreated", table: table)
        case .emailSent: MonarchStrings.string("monarch.aiToolResultCard.kind.emailSent", table: table)
        case .eventBooked: MonarchStrings.string("monarch.aiToolResultCard.kind.eventBooked", table: table)
        case .ticketFiled: MonarchStrings.string("monarch.aiToolResultCard.kind.ticketFiled", table: table)
        case .generic: MonarchStrings.string("monarch.aiToolResultCard.kind.generic", table: table)
        }
    }
    /// The link that opens the result.
    static var open: String { MonarchStrings.string("monarch.aiToolResultCard.open", table: table) }
    /// The undo button.
    static var undo: String { MonarchStrings.string("monarch.aiToolResultCard.undo", table: table) }
    /// Shown after a successful undo.
    static var undone: String { MonarchStrings.string("monarch.aiToolResultCard.undone", table: table) }
    /// Shown when the undo fails.
    static var undoFailed: String { MonarchStrings.string("monarch.aiToolResultCard.undoFailed", table: table) }
}
