/// Replacements for the built-in strings of ``MonarchAiMemoryPanel``.
/// Every value is optional; leave one out to keep the built-in English text.
@_spi(Experimental) public struct MonarchAiMemoryPanelLabels: Sendable {
    /// The spoken name of the list.
    public var label: String
    /// The prompt of the search field.
    public var search: String
    /// The toolbar button that starts selecting.
    public var select: String
    /// The toolbar button that stops selecting.
    public var done: String
    /// The toolbar button that selects every memory shown, given how many are shown.
    public var selectAll: @Sendable (_ shown: Int) -> String
    /// The toolbar button that clears the selection when everything shown is selected.
    public var deselectAll: String
    /// The name of an entity kind, shown under the group name.
    public var kind: @Sendable (_ kind: MonarchAiMemoryEntityKind) -> String
    /// How many memories a group has.
    public var count: @Sendable (_ count: Int) -> String
    /// The line under a memory, given the formatted date.
    public var updated: @Sendable (_ date: String) -> String
    /// Shown when nothing is remembered.
    public var empty: String
    /// Shown when the search matches nothing, given the search.
    public var noMatches: @Sendable (_ query: String) -> String
    /// Announced after a search, given the number of matches.
    public var matches: @Sendable (_ count: Int) -> String
    /// The forget command: the swipe action and the bulk action.
    public var forget: String
    /// The disabled bulk command shown in selection mode before anything is selected. Built in: "Forget selected".
    public var forgetSelected: String
    /// The title of the bulk forget confirmation, given the number of memories.
    public var confirmTitle: @Sendable (_ count: Int) -> String
    /// The text of the bulk forget confirmation.
    public var confirmMessage: String
    /// The button that dismisses the confirmation.
    public var cancel: String
    /// Announced after memories are forgotten, given how many.
    public var forgotten: @Sendable (_ count: Int) -> String
    /// Announced after a memory is edited.
    public var edited: String
    /// The strings of each row's ``MonarchAiMemoryChip``.
    public var chip: MonarchAiMemoryChipLabels
    /// The strings of the ``MonarchBulkActionBar`` shown while memories are selected.
    public var bulkBar: MonarchBulkActionBarLabels

    /// Labels with optional replacements.
    ///
    /// - Parameters:
    ///   - label: Replaces "Memories".
    ///   - search: Replaces "Search memories".
    ///   - select: Replaces "Select".
    ///   - done: Replaces "Done".
    ///   - selectAll: Replaces "Select all (4)".
    ///   - deselectAll: Replaces "Deselect all".
    ///   - kind: Replaces "Person", "Organization", "Project" and "Other".
    ///   - count: Replaces "3 memories".
    ///   - updated: Replaces "Updated …".
    ///   - empty: Replaces "Nothing is remembered yet.".
    ///   - noMatches: Replaces "No memories match “…”.".
    ///   - matches: Replaces "2 memories match".
    ///   - forget: Replaces "Forget".
    ///   - confirmTitle: Replaces "Forget 2 memories?".
    ///   - confirmMessage: Replaces the confirmation text.
    ///   - cancel: Replaces "Cancel".
    ///   - forgotten: Replaces "2 memories forgotten".
    ///   - edited: Replaces "Memory updated".
    ///   - chip: The row strings.
    ///   - bulkBar: The bulk action bar strings.
    ///   - forgetSelected: Replaces "Forget selected".
    public init(
        label: String? = nil,
        search: String? = nil,
        select: String? = nil,
        done: String? = nil,
        selectAll: (@Sendable (_ shown: Int) -> String)? = nil,
        deselectAll: String? = nil,
        kind: (@Sendable (_ kind: MonarchAiMemoryEntityKind) -> String)? = nil,
        count: (@Sendable (_ count: Int) -> String)? = nil,
        updated: (@Sendable (_ date: String) -> String)? = nil,
        empty: String? = nil,
        noMatches: (@Sendable (_ query: String) -> String)? = nil,
        matches: (@Sendable (_ count: Int) -> String)? = nil,
        forget: String? = nil,
        confirmTitle: (@Sendable (_ count: Int) -> String)? = nil,
        confirmMessage: String? = nil,
        cancel: String? = nil,
        forgotten: (@Sendable (_ count: Int) -> String)? = nil,
        edited: String? = nil,
        chip: MonarchAiMemoryChipLabels = MonarchAiMemoryChipLabels(),
        bulkBar: MonarchBulkActionBarLabels = MonarchBulkActionBarLabels(),
        forgetSelected: String? = nil
    ) {
        typealias Strings = MonarchAiMemoryPanelStrings
        self.label = label ?? Strings.label
        self.search = search ?? Strings.search
        self.select = select ?? Strings.select
        self.done = done ?? Strings.done
        self.selectAll = selectAll ?? { Strings.selectAll($0) }
        self.deselectAll = deselectAll ?? Strings.deselectAll
        self.kind = kind ?? { Strings.kind($0) }
        self.count = count ?? { Strings.count($0) }
        self.updated = updated ?? { Strings.updated($0) }
        self.empty = empty ?? Strings.empty
        self.noMatches = noMatches ?? { Strings.noMatches($0) }
        self.matches = matches ?? { Strings.matches($0) }
        self.forget = forget ?? Strings.forget
        self.confirmTitle = confirmTitle ?? { Strings.confirmTitle($0) }
        self.confirmMessage = confirmMessage ?? Strings.confirmMessage
        self.cancel = cancel ?? Strings.cancel
        self.forgotten = forgotten ?? { Strings.forgotten($0) }
        self.edited = edited ?? Strings.edited
        self.chip = chip
        self.bulkBar = bulkBar
        self.forgetSelected = forgetSelected ?? Strings.forgetSelected
    }
}
