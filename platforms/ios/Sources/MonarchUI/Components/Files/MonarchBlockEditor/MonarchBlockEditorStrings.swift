/// Built-in strings of ``MonarchBlockEditor``, from the `MonarchBlockEditor` strings table.
enum MonarchBlockEditorStrings {
    private static let table = "MonarchBlockEditor"

    /// A string of the table by its short key.
    static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.blockEditor.\(key)", table: table)
    }

    /// The placeholder of an empty document.
    static var prompt: String { string("prompt") }
    /// The spoken hint of a block's action button.
    static var handleHint: String { string("handleHint") }
    /// The action that moves a block up.
    static var moveUp: String { string("moveUp") }
    /// The action that moves a block down.
    static var moveDown: String { string("moveDown") }
    /// The action that duplicates blocks.
    static var duplicate: String { string("duplicate") }
    /// The action that deletes one block.
    static var deleteBlock: String { string("deleteBlock") }
    /// The action that deletes the selected blocks.
    static var deleteSelected: String { string("deleteSelected") }
    /// The menu that changes a block's kind.
    static var turnInto: String { string("turnInto") }
    /// The menu that changes a callout's tone.
    static var tone: String { string("tone") }
    /// The neutral tone's name.
    static var toneNeutral: String { string("toneNeutral") }
    /// The button that starts selecting blocks.
    static var select: String { string("select") }
    /// The button that ends selecting blocks.
    static var done: String { string("done") }
    /// The placeholder of a toggle's summary.
    static var summaryPrompt: String { string("summaryPrompt") }
    /// The placeholder of a toggle's content.
    static var detailPrompt: String { string("detailPrompt") }
    /// The placeholder of an embed's link.
    static var embedPrompt: String { string("embedPrompt") }
    /// The button that adds an empty paragraph at the end.
    static var addBlock: String { string("addBlock") }
    /// The slash menu group of text blocks.
    static var groupText: String { string("groupText") }
    /// The slash menu group of lists.
    static var groupLists: String { string("groupLists") }
    /// The slash menu group of other blocks.
    static var groupBlocks: String { string("groupBlocks") }

    /// The spoken name of a block's action button.
    static func handle(_ block: String) -> String {
        MonarchStrings.string("monarch.blockEditor.handle", table: table, block)
    }

    /// How many blocks are selected.
    static func selectedCount(_ count: Int) -> String {
        MonarchStrings.string("monarch.blockEditor.selectedCount", table: table, count)
    }

    /// The spoken name of a list item.
    static func listItem(_ list: String, position: Int) -> String {
        MonarchStrings.string("monarch.blockEditor.listItem", table: table, list, position)
    }
}
