/// Replacements for the built-in strings of ``MonarchAiConversationHeader``.
///
/// Every property is `nil` by default, which keeps the built-in string.
@_spi(Experimental) public struct MonarchAiConversationHeaderLabels: Sendable {
    /// Shown when the title is empty. Built in: "New conversation".
    public var untitled: String?
    /// The menu item and the title of the rename prompt. Built in: "Rename conversation".
    public var rename: String?
    /// The placeholder and spoken name of the title field. Built in: "Conversation title".
    public var titleLabel: String?
    /// The button that saves a rename. Built in: "Save".
    public var save: String?
    /// The button that cancels a rename. Built in: "Cancel".
    public var cancel: String?
    /// Shown while the title is being generated. Built in: "Naming conversation…".
    public var naming: String?
    /// Read before the agent's name. Built in: "AI assistant".
    public var agentPrefix: String?
    /// Read before the model. Built in: "Model".
    public var modelPrefix: String?
    /// Shown in the rename prompt and announced when a rename fails. Built in: "Couldn’t rename. Try again."
    public var renameFailed: String?
    /// The hint of the title when it opens a menu. Built in: "Shows conversation actions".
    public var menuHint: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        untitled: String? = nil,
        rename: String? = nil,
        titleLabel: String? = nil,
        save: String? = nil,
        cancel: String? = nil,
        naming: String? = nil,
        agentPrefix: String? = nil,
        modelPrefix: String? = nil,
        renameFailed: String? = nil,
        menuHint: String? = nil
    ) {
        self.untitled = untitled
        self.rename = rename
        self.titleLabel = titleLabel
        self.save = save
        self.cancel = cancel
        self.naming = naming
        self.agentPrefix = agentPrefix
        self.modelPrefix = modelPrefix
        self.renameFailed = renameFailed
        self.menuHint = menuHint
    }
}
