import Foundation

/// One agent of a run: the lead, or a sub-agent it handed work to.
@_spi(Experimental) public struct MonarchAiSubagent: Identifiable, Sendable, Hashable {
    /// A stable id, unique across the whole tree.
    public var id: String
    /// The display name, such as "Researcher".
    public var name: String
    /// Its role or assignment, such as "Find the three latest filings".
    public var role: String?
    /// Where the agent is.
    public var status: MonarchAiSubagentStatus
    /// What it is doing right now, shown while it is working.
    public var activity: String?
    /// Steps finished so far.
    public var stepsDone: Int?
    /// Steps planned in total; `nil` when unknown.
    public var stepsTotal: Int?
    /// Tool calls made so far.
    public var toolCalls: Int?
    /// When it started.
    public var startedAt: Date?
    /// When it ended.
    public var endedAt: Date?
    /// What it produced or why it ended, shown once it is done, failed or stopped.
    public var summary: String?
    /// The agents it handed work to, nested to any depth.
    public var children: [MonarchAiSubagent]
    /// The conversation part that started this agent, such as the hand-off's tool call id.
    public var partId: String?
    /// The conversation message this agent belongs to.
    public var messageId: String?

    /// An agent.
    ///
    /// - Parameters:
    ///   - id: A stable id, unique across the whole tree.
    ///   - name: The display name.
    ///   - role: Its role or assignment.
    ///   - status: Where the agent is.
    ///   - activity: What it is doing right now.
    ///   - stepsDone: Steps finished so far.
    ///   - stepsTotal: Steps planned in total.
    ///   - toolCalls: Tool calls made so far.
    ///   - startedAt: When it started.
    ///   - endedAt: When it ended.
    ///   - summary: What it produced or why it ended.
    ///   - children: The agents it handed work to.
    ///   - partId: The conversation part that started it.
    ///   - messageId: The conversation message it belongs to.
    public init(
        id: String,
        name: String,
        role: String? = nil,
        status: MonarchAiSubagentStatus,
        activity: String? = nil,
        stepsDone: Int? = nil,
        stepsTotal: Int? = nil,
        toolCalls: Int? = nil,
        startedAt: Date? = nil,
        endedAt: Date? = nil,
        summary: String? = nil,
        children: [MonarchAiSubagent] = [],
        partId: String? = nil,
        messageId: String? = nil
    ) {
        self.id = id
        self.name = name
        self.role = role
        self.status = status
        self.activity = activity
        self.stepsDone = stepsDone
        self.stepsTotal = stepsTotal
        self.toolCalls = toolCalls
        self.startedAt = startedAt
        self.endedAt = endedAt
        self.summary = summary
        self.children = children
        self.partId = partId
        self.messageId = messageId
    }
}
