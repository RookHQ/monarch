/// Replacements for the built-in strings of ``MonarchEditorToolbar``.
///
/// Every property is `nil` or empty by default, which keeps the built-in string.
public struct MonarchEditorToolbarLabels: Sendable {
    /// The spoken name of the toolbar. Built in: "Formatting".
    public var toolbar: String?
    /// Names of tools, by tool, such as `[.bold: "Gras"]`. Built in: "Bold", "Heading 2" and so on.
    public var tools: [MonarchEditorTool: String]
    /// The text style menu. Built in: "Text style".
    public var textStyle: String?
    /// The title of the link form. Built in: "Link".
    public var linkTitle: String?
    /// The address field of the link form. Built in: "Link address".
    public var linkAddress: String?
    /// The button that applies the link. Built in: "Apply".
    public var linkApply: String?
    /// The button that removes the link. Built in: "Remove link".
    public var linkRemove: String?
    /// The button that closes the link form. Built in: "Cancel".
    public var linkCancel: String?
    /// The help under the link form. Built in: "Enter a web address (https://…) or an email address."
    public var linkHelp: String?

    /// Replacements for built-in strings. Pass only the ones to replace.
    public init(
        toolbar: String? = nil,
        tools: [MonarchEditorTool: String] = [:],
        textStyle: String? = nil,
        linkTitle: String? = nil,
        linkAddress: String? = nil,
        linkApply: String? = nil,
        linkRemove: String? = nil,
        linkCancel: String? = nil,
        linkHelp: String? = nil
    ) {
        self.toolbar = toolbar
        self.tools = tools
        self.textStyle = textStyle
        self.linkTitle = linkTitle
        self.linkAddress = linkAddress
        self.linkApply = linkApply
        self.linkRemove = linkRemove
        self.linkCancel = linkCancel
        self.linkHelp = linkHelp
    }
}
