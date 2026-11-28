/// Replacements for the built-in strings of ``MonarchAiDraftQueue``. Every
/// value is optional; leave one out to keep the built-in English text.
public struct MonarchAiDraftQueueLabels: Sendable {
    /// The spoken name of the queue when it has no title.
    public var label: String
    /// The progress line: "2 of 5 reviewed".
    public var progress: @Sendable (_ reviewed: Int, _ total: Int) -> String
    /// The title shown when there are no drafts.
    public var empty: String
    /// The name of a status, shown with an icon.
    public var status: @Sendable (_ status: MonarchAiDraftStatus) -> String
    /// Shown instead of the body while a draft is being written again.
    public var writing: String
    /// Read before each body, so it is identified as AI-written.
    public var aiDraft: String
    /// The heading of the context.
    public var context: String
    /// The approve button.
    public var approve: String
    /// The send button.
    public var send: String
    /// The edit button.
    public var edit: String
    /// The regenerate button.
    public var regenerate: String
    /// The skip button.
    public var skip: String
    /// The save button of the editor.
    public var save: String
    /// The cancel button of the editor.
    public var cancel: String
    /// The name of the editor, from the draft's title.
    public var editField: @Sendable (_ title: String) -> String
    /// Announced when a draft's status changes.
    public var announce: @Sendable (_ status: MonarchAiDraftStatus, _ title: String) -> String

    /// Labels with optional replacements.
    ///
    /// - Parameters:
    ///   - label: Replaces "Drafts to review".
    ///   - progress: Replaces "… of … reviewed".
    ///   - empty: Replaces "No drafts to review".
    ///   - status: Replaces the status names.
    ///   - writing: Replaces "AI is still writing…".
    ///   - aiDraft: Replaces "AI draft".
    ///   - context: Replaces "Context".
    ///   - approve: Replaces "Approve".
    ///   - send: Replaces "Send".
    ///   - edit: Replaces "Edit".
    ///   - regenerate: Replaces "Regenerate".
    ///   - skip: Replaces "Skip".
    ///   - save: Replaces "Save".
    ///   - cancel: Replaces "Cancel".
    ///   - editField: Replaces "Edit draft: …".
    ///   - announce: Replaces the status announcements.
    public init(
        label: String? = nil,
        progress: (@Sendable (_ reviewed: Int, _ total: Int) -> String)? = nil,
        empty: String? = nil,
        status: (@Sendable (_ status: MonarchAiDraftStatus) -> String)? = nil,
        writing: String? = nil,
        aiDraft: String? = nil,
        context: String? = nil,
        approve: String? = nil,
        send: String? = nil,
        edit: String? = nil,
        regenerate: String? = nil,
        skip: String? = nil,
        save: String? = nil,
        cancel: String? = nil,
        editField: (@Sendable (_ title: String) -> String)? = nil,
        announce: (@Sendable (_ status: MonarchAiDraftStatus, _ title: String) -> String)? = nil
    ) {
        typealias Strings = MonarchAiDraftQueueStrings
        self.label = label ?? Strings.label
        self.progress = progress ?? { Strings.progress($0, $1) }
        self.empty = empty ?? Strings.empty
        let statusName = status ?? { Strings.status($0) }
        self.status = statusName
        self.writing = writing ?? Strings.writing
        self.aiDraft = aiDraft ?? Strings.aiDraft
        self.context = context ?? Strings.context
        self.approve = approve ?? Strings.approve
        self.send = send ?? Strings.send
        self.edit = edit ?? Strings.edit
        self.regenerate = regenerate ?? Strings.regenerate
        self.skip = skip ?? Strings.skip
        self.save = save ?? Strings.save
        self.cancel = cancel ?? Strings.cancel
        self.editField = editField ?? { Strings.editField($0) }
        self.announce = announce ?? { Strings.announce($0, title: $1, statusName: statusName($0)) }
    }
}
