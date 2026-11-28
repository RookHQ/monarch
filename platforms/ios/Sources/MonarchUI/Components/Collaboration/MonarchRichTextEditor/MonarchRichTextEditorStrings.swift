/// Built-in strings of the rich-text editor, from the `MonarchRichTextEditor` strings table.
enum MonarchRichTextEditorStrings {
    private static let table = "MonarchRichTextEditor"

    /// The number of characters, without a limit.
    static func count(_ count: Int) -> String {
        count == 1
            ? MonarchStrings.string("monarch.richTextEditor.countOne", table: table)
            : MonarchStrings.string("monarch.richTextEditor.countOther", table: table, count)
    }

    /// The number of characters against a limit.
    static func countOfLimit(_ count: Int, limit: Int) -> String {
        MonarchStrings.string("monarch.richTextEditor.countOfLimit", table: table, count, limit)
    }

    /// The characters left before the limit, or over it when negative.
    static func remaining(_ left: Int) -> String {
        switch left {
        case 1: MonarchStrings.string("monarch.richTextEditor.remainingOne", table: table)
        case -1: MonarchStrings.string("monarch.richTextEditor.overOne", table: table)
        case ..<0: MonarchStrings.string("monarch.richTextEditor.overOther", table: table, -left)
        default: MonarchStrings.string("monarch.richTextEditor.remainingOther", table: table, left)
        }
    }
}
