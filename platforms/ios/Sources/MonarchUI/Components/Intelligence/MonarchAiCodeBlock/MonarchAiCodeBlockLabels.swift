/// Replacements for the built-in strings of ``MonarchAiCodeBlock``.
///
/// Every property is `nil` by default, which keeps the built-in string.
public struct MonarchAiCodeBlockLabels: Sendable {
    /// The title of the apply button. Built in: "Apply".
    public var apply: String?
    /// The title of the insert button. Built in: "Insert".
    public var insert: String?
    /// The spoken name of an action, from its title and the name of the code.
    /// Built in: "Apply server.ts".
    public var actionLabel: (@Sendable (String, String) -> String)?
    /// The hint of Apply and Insert while the code is still arriving.
    /// Built in: "Available when the code is complete".
    public var incompleteHint: String?
    /// The strings of the code block itself: copy, wrap, diff lines and the code's name.
    public var codeBlock: MonarchCodeBlockLabels

    /// Replacements for built-in strings. Pass only the ones to replace.
    ///
    /// - Parameters:
    ///   - apply: The title of the apply button.
    ///   - insert: The title of the insert button.
    ///   - actionLabel: The spoken name of an action, from its title and the code's name.
    ///   - incompleteHint: The hint of the actions while the code is still arriving.
    ///   - codeBlock: The strings of the code block itself.
    public init(
        apply: String? = nil,
        insert: String? = nil,
        actionLabel: (@Sendable (String, String) -> String)? = nil,
        incompleteHint: String? = nil,
        codeBlock: MonarchCodeBlockLabels = MonarchCodeBlockLabels()
    ) {
        self.apply = apply
        self.insert = insert
        self.actionLabel = actionLabel
        self.incompleteHint = incompleteHint
        self.codeBlock = codeBlock
    }
}
