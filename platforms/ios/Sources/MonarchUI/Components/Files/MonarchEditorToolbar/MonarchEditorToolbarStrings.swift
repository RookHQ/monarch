/// Built-in strings of the editor toolbar, from the `MonarchEditorToolbar` strings table.
enum MonarchEditorToolbarStrings {
    private static let table = "MonarchEditorToolbar"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.editorToolbar.\(key)", table: table)
    }

    /// The spoken name of the toolbar.
    static var toolbar: String { string("toolbar") }
    /// The text style menu.
    static var textStyle: String { string("textStyle") }
    /// The title of the link form.
    static var linkTitle: String { string("linkTitle") }
    /// The address field.
    static var linkAddress: String { string("linkAddress") }
    /// Applies the link.
    static var linkApply: String { string("linkApply") }
    /// Removes the link.
    static var linkRemove: String { string("linkRemove") }
    /// Closes the link form.
    static var linkCancel: String { string("linkCancel") }
    /// The help under the link form.
    static var linkHelp: String { string("linkHelp") }

    /// The built-in name of a tool.
    static func title(_ tool: MonarchEditorTool) -> String {
        switch tool {
        case .undo: string("undo")
        case .redo: string("redo")
        case .paragraph: string("paragraph")
        case .heading(let level):
            MonarchStrings.string("monarch.editorToolbar.heading", table: table, level)
        case .bold: string("bold")
        case .italic: string("italic")
        case .strikethrough: string("strikethrough")
        case .code: string("code")
        case .link: string("link")
        case .bulletList: string("bulletList")
        case .orderedList: string("orderedList")
        case .taskList: string("taskList")
        case .blockquote: string("blockquote")
        case .codeBlock: string("codeBlock")
        case .custom(_, let title, _): title
        }
    }
}
