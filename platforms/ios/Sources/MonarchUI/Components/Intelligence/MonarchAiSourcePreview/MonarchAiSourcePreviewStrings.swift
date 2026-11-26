/// Built-in strings of the AI source preview, from the `MonarchAiSourcePreview` strings table.
enum MonarchAiSourcePreviewStrings {
    private static let table = "MonarchAiSourcePreview"

    /// The caption of the quoted passage.
    static var quoted: String { MonarchStrings.string("monarch.aiSourcePreview.quoted", table: table) }
    /// The button that opens the original.
    static var openOriginal: String { MonarchStrings.string("monarch.aiSourcePreview.openOriginal", table: table) }
    /// Shown when there is nothing to preview.
    static var noPreview: String { MonarchStrings.string("monarch.aiSourcePreview.noPreview", table: table) }
    /// The button that closes the preview.
    static var close: String { MonarchStrings.string("monarch.aiSourcePreview.close", table: table) }
}
