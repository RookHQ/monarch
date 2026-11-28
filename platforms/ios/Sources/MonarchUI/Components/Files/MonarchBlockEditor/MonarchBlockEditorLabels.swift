/// Replacements for the built-in strings of ``MonarchBlockEditor``.
/// A property that is `nil` (or a missing dictionary entry) keeps the
/// built-in English string.
@_spi(Experimental) public struct MonarchBlockEditorLabels: Sendable {
    /// The spoken name of a block's action button, from the block's kind name. Built in: "Block actions: Heading 2".
    public var handle: (@Sendable (String) -> String)?
    /// The spoken hint of a block's action button.
    public var handleHint: String?
    /// Built in: "Move up".
    public var moveUp: String?
    /// Built in: "Move down".
    public var moveDown: String?
    /// Built in: "Duplicate".
    public var duplicate: String?
    /// Built in: "Delete block".
    public var deleteBlock: String?
    /// The action that deletes the selected blocks. Built in: "Delete".
    public var deleteSelected: String?
    /// Built in: "Turn into".
    public var turnInto: String?
    /// Built in: "Tone".
    public var tone: String?
    /// Names of tones in the callout tone menu. Built in: "Neutral" and the shared tone names.
    public var toneNames: [MonarchTone: String]
    /// Built in: "Select blocks".
    public var select: String?
    /// Built in: "Done".
    public var done: String?
    /// How many blocks are selected. Built in: "3 selected".
    public var selectedCount: (@Sendable (Int) -> String)?
    /// The spoken name of a list item, from the list's name and the position. Built in: "Numbered list, item 2".
    public var listItem: (@Sendable (String, Int) -> String)?
    /// Names of block kinds, used in the slash menu, the Turn into menu and
    /// spoken names. Callouts of every tone use the `.callout(.neutral)` key.
    public var blockNames: [MonarchBlockKind: String]
    /// Slash menu descriptions of block kinds, keyed like `blockNames`.
    public var blockDescriptions: [MonarchBlockKind: String]
    /// Slash menu group headings. Built in: "Text", "Lists" and "Blocks".
    public var groupText: String?
    /// The heading of the list group of the slash menu.
    public var groupLists: String?
    /// The heading of the other blocks of the slash menu.
    public var groupBlocks: String?
    /// The placeholder of a toggle's summary. Built in: "Summary".
    public var summaryPrompt: String?
    /// The placeholder of a toggle's content. Built in: "Content".
    public var detailPrompt: String?
    /// The placeholder of an embed's link. Built in: "Paste a link to embed".
    public var embedPrompt: String?
    /// The button that adds an empty paragraph at the end. Built in: "Add a block".
    public var addBlock: String?
    /// The strings of the toolbar.
    public var toolbar: MonarchEditorToolbarLabels
    /// The strings of the slash menu.
    public var slashMenu: MonarchSlashMenuLabels
    /// The strings of table blocks.
    public var table: MonarchTableBlockLabels
    /// The strings of embed blocks.
    public var embed: MonarchEmbedBlockLabels

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        handle: (@Sendable (String) -> String)? = nil,
        handleHint: String? = nil,
        moveUp: String? = nil,
        moveDown: String? = nil,
        duplicate: String? = nil,
        deleteBlock: String? = nil,
        deleteSelected: String? = nil,
        turnInto: String? = nil,
        tone: String? = nil,
        toneNames: [MonarchTone: String] = [:],
        select: String? = nil,
        done: String? = nil,
        selectedCount: (@Sendable (Int) -> String)? = nil,
        listItem: (@Sendable (String, Int) -> String)? = nil,
        blockNames: [MonarchBlockKind: String] = [:],
        blockDescriptions: [MonarchBlockKind: String] = [:],
        groupText: String? = nil,
        groupLists: String? = nil,
        groupBlocks: String? = nil,
        summaryPrompt: String? = nil,
        detailPrompt: String? = nil,
        embedPrompt: String? = nil,
        addBlock: String? = nil,
        toolbar: MonarchEditorToolbarLabels = MonarchEditorToolbarLabels(),
        slashMenu: MonarchSlashMenuLabels = MonarchSlashMenuLabels(),
        table: MonarchTableBlockLabels = MonarchTableBlockLabels(),
        embed: MonarchEmbedBlockLabels = MonarchEmbedBlockLabels()
    ) {
        self.handle = handle
        self.handleHint = handleHint
        self.moveUp = moveUp
        self.moveDown = moveDown
        self.duplicate = duplicate
        self.deleteBlock = deleteBlock
        self.deleteSelected = deleteSelected
        self.turnInto = turnInto
        self.tone = tone
        self.toneNames = toneNames
        self.select = select
        self.done = done
        self.selectedCount = selectedCount
        self.listItem = listItem
        self.blockNames = blockNames
        self.blockDescriptions = blockDescriptions
        self.groupText = groupText
        self.groupLists = groupLists
        self.groupBlocks = groupBlocks
        self.summaryPrompt = summaryPrompt
        self.detailPrompt = detailPrompt
        self.embedPrompt = embedPrompt
        self.addBlock = addBlock
        self.toolbar = toolbar
        self.slashMenu = slashMenu
        self.table = table
        self.embed = embed
    }
}
