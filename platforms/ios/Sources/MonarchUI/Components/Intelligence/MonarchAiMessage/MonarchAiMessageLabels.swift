import Foundation

/// Replacements for the built-in strings of ``MonarchAiMessage`` and of the
/// components it draws its parts with.
///
/// Every property is `nil` or empty by default, which keeps the built-in strings.
@_spi(Experimental) public struct MonarchAiMessageLabels: Sendable {
    /// The spoken name of an assistant message. Built in: "Assistant said".
    public var assistantSaid: String?
    /// The spoken name of the person's message. Built in: "You said".
    public var youSaid: String?
    /// The spoken name of a system or tool message. Built in: "System message".
    public var systemSaid: String?
    /// The name in the header when the message has no agent. Built in: "Assistant".
    public var assistant: String?
    /// The notice under a reply that was cut short, filtered or refused. Return `nil` for the built-in text.
    public var finishNotice: (@Sendable (MonarchAiFinishReason) -> String?)?
    /// The line under a message written offline. Built in: "Queued. It will send when you are back online."
    public var queued: String?
    /// The button that cancels a queued message. Built in: "Cancel".
    public var cancelQueued: String?
    /// The button that resumes a dropped reply. Built in: "Resume".
    public var resume: String?
    /// The name of the plan's steps. Built in: "Plan".
    public var plan: String?
    /// The branch switcher's name, buttons and position.
    public var versions: String?
    /// The previous version button.
    public var previousVersion: String?
    /// The next version button.
    public var nextVersion: String?
    /// The visible position, such as "2 / 3".
    public var versionOf: (@Sendable (Int, Int) -> String)?
    /// The edit form's field name.
    public var editLabel: String?
    /// The edit form's save button.
    public var save: String?
    /// The edit form's cancel button.
    public var cancel: String?
    /// The strings of the text parts.
    public var streamingText: MonarchAiStreamingTextLabels
    /// The strings of the reasoning parts.
    public var reasoning: MonarchAiReasoningLabels
    /// The strings of the tool calls.
    public var toolCall: MonarchAiToolCallLabels
    /// The strings of the tool approvals.
    public var toolApproval: MonarchAiToolApprovalLabels
    /// The strings of the source list.
    public var sources: MonarchAiSourceListLabels
    /// The strings of the citation markers.
    public var citation: MonarchAiCitationLabels
    /// The strings of the file chips.
    public var attachment: MonarchAttachmentChipLabels
    /// The strings of the memory chips.
    public var memory: MonarchAiMemoryChipLabels
    /// The strings of the error part.
    public var error: MonarchAiErrorMessageLabels
    /// The strings of the actions row.
    public var actions: MonarchAiMessageActionsLabels

    /// Replacements for built-in strings. Pass only the ones to replace.
    ///
    /// - Parameters:
    ///   - assistantSaid: The spoken name of an assistant message.
    ///   - youSaid: The spoken name of the person's message.
    ///   - systemSaid: The spoken name of a system or tool message.
    ///   - assistant: The name in the header when the message has no agent.
    ///   - finishNotice: The notice under a reply that was cut short, filtered or refused.
    ///   - queued: The line under a message written offline.
    ///   - cancelQueued: The button that cancels a queued message.
    ///   - resume: The button that resumes a dropped reply.
    ///   - plan: The name of the plan's steps.
    ///   - versions: The branch switcher's name.
    ///   - previousVersion: The previous version button.
    ///   - nextVersion: The next version button.
    ///   - versionOf: The visible position of the branch switcher.
    ///   - editLabel: The edit form's field name.
    ///   - save: The edit form's save button.
    ///   - cancel: The edit form's cancel button.
    ///   - streamingText: The strings of the text parts.
    ///   - reasoning: The strings of the reasoning parts.
    ///   - toolCall: The strings of the tool calls.
    ///   - toolApproval: The strings of the tool approvals.
    ///   - sources: The strings of the source list.
    ///   - citation: The strings of the citation markers.
    ///   - attachment: The strings of the file chips.
    ///   - memory: The strings of the memory chips.
    ///   - error: The strings of the error part.
    ///   - actions: The strings of the actions row.
    public init(
        assistantSaid: String? = nil,
        youSaid: String? = nil,
        systemSaid: String? = nil,
        assistant: String? = nil,
        finishNotice: (@Sendable (MonarchAiFinishReason) -> String?)? = nil,
        queued: String? = nil,
        cancelQueued: String? = nil,
        resume: String? = nil,
        plan: String? = nil,
        versions: String? = nil,
        previousVersion: String? = nil,
        nextVersion: String? = nil,
        versionOf: (@Sendable (Int, Int) -> String)? = nil,
        editLabel: String? = nil,
        save: String? = nil,
        cancel: String? = nil,
        streamingText: MonarchAiStreamingTextLabels = MonarchAiStreamingTextLabels(),
        reasoning: MonarchAiReasoningLabels = MonarchAiReasoningLabels(),
        toolCall: MonarchAiToolCallLabels = MonarchAiToolCallLabels(),
        toolApproval: MonarchAiToolApprovalLabels = MonarchAiToolApprovalLabels(),
        sources: MonarchAiSourceListLabels = MonarchAiSourceListLabels(),
        citation: MonarchAiCitationLabels = MonarchAiCitationLabels(),
        attachment: MonarchAttachmentChipLabels = MonarchAttachmentChipLabels(),
        memory: MonarchAiMemoryChipLabels = MonarchAiMemoryChipLabels(),
        error: MonarchAiErrorMessageLabels = MonarchAiErrorMessageLabels(),
        actions: MonarchAiMessageActionsLabels = MonarchAiMessageActionsLabels()
    ) {
        self.assistantSaid = assistantSaid
        self.youSaid = youSaid
        self.systemSaid = systemSaid
        self.assistant = assistant
        self.finishNotice = finishNotice
        self.queued = queued
        self.cancelQueued = cancelQueued
        self.resume = resume
        self.plan = plan
        self.versions = versions
        self.previousVersion = previousVersion
        self.nextVersion = nextVersion
        self.versionOf = versionOf
        self.editLabel = editLabel
        self.save = save
        self.cancel = cancel
        self.streamingText = streamingText
        self.reasoning = reasoning
        self.toolCall = toolCall
        self.toolApproval = toolApproval
        self.sources = sources
        self.citation = citation
        self.attachment = attachment
        self.memory = memory
        self.error = error
        self.actions = actions
    }
}
