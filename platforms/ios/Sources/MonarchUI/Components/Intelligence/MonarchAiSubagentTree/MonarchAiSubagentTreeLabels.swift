import Foundation

/// Replacements for the built-in strings of ``MonarchAiSubagentTree``.
///
/// Every property is `nil` by default, which keeps the built-in string.
@_spi(Experimental) public struct MonarchAiSubagentTreeLabels: Sendable {
    /// The spoken name of the tree. Built in: "Agents".
    public var label: String?
    /// The name of a status. Built in: "Working", "Needs input", "Waiting",
    /// "Done", "Failed", "Stopped".
    public var status: (@Sendable (MonarchAiSubagentStatus) -> String)?
    /// The total in the header, from the count. Built in: "4 agents".
    public var agentCount: (@Sendable (Int) -> String)?
    /// One part of the header breakdown, from a status and its count.
    /// Built in: "2 working", "1 needs you".
    public var statusCount: (@Sendable (MonarchAiSubagentStatus, Int) -> String)?
    /// Shown instead of the tree when there are no agents. Built in: "No agents yet".
    public var empty: String?
    /// Progress, from the steps done and the total if known.
    /// Built in: "3 of 7 steps", "3 steps".
    public var steps: (@Sendable (Int, Int?) -> String)?
    /// Tool calls, from the count. Built in: "12 tool calls".
    public var toolCalls: (@Sendable (Int) -> String)?
    /// The start of an agent that has not ended, from the formatted time.
    /// Built in: "Started 12:01".
    public var started: (@Sendable (String) -> String)?
    /// The start and end of an agent that has ended. Built in: "12:01 to 12:04".
    public var ran: (@Sendable (String, String) -> String)?
    /// The spoken name of the action bar, from the agent's name. Built in: "Actions for Researcher".
    public var actionsFor: (@Sendable (String) -> String)?
    /// The Stop button. Built in: "Stop".
    public var stop: String?
    /// The Open button. Built in: "Open".
    public var open: String?
    /// The Respond button. Built in: "Respond".
    public var respond: String?
    /// The button that shows the agent's conversation part. Built in: "Show in conversation".
    public var openPart: String?
    /// Announced when an agent's status changes, from its name and status.
    /// Built in: "Researcher: Done".
    public var announce: (@Sendable (String, String) -> String)?
    /// The spoken name of the button that opens an agent, from its name. Built in: "Show agents under Researcher".
    public var expand: (@Sendable (String) -> String)?
    /// The spoken name of the button that closes an agent, from its name. Built in: "Hide agents under Researcher".
    public var collapse: (@Sendable (String) -> String)?

    /// Replacements for built-in strings. Pass only the ones to replace.
    ///
    /// - Parameters:
    ///   - label: The spoken name of the tree.
    ///   - status: The name of a status.
    ///   - agentCount: The total in the header.
    ///   - statusCount: One part of the header breakdown.
    ///   - empty: Shown when there are no agents.
    ///   - steps: Progress in steps.
    ///   - toolCalls: The number of tool calls.
    ///   - started: The start of an agent that has not ended.
    ///   - ran: The start and end of an agent that has ended.
    ///   - actionsFor: The spoken name of the action bar.
    ///   - stop: The Stop button.
    ///   - open: The Open button.
    ///   - respond: The Respond button.
    ///   - openPart: The button that shows the agent's conversation part.
    ///   - announce: Announced when an agent's status changes.
    ///   - expand: The spoken name of the button that opens an agent.
    ///   - collapse: The spoken name of the button that closes an agent.
    public init(
        label: String? = nil,
        status: (@Sendable (MonarchAiSubagentStatus) -> String)? = nil,
        agentCount: (@Sendable (Int) -> String)? = nil,
        statusCount: (@Sendable (MonarchAiSubagentStatus, Int) -> String)? = nil,
        empty: String? = nil,
        steps: (@Sendable (Int, Int?) -> String)? = nil,
        toolCalls: (@Sendable (Int) -> String)? = nil,
        started: (@Sendable (String) -> String)? = nil,
        ran: (@Sendable (String, String) -> String)? = nil,
        actionsFor: (@Sendable (String) -> String)? = nil,
        stop: String? = nil,
        open: String? = nil,
        respond: String? = nil,
        openPart: String? = nil,
        announce: (@Sendable (String, String) -> String)? = nil,
        expand: (@Sendable (String) -> String)? = nil,
        collapse: (@Sendable (String) -> String)? = nil
    ) {
        self.label = label
        self.status = status
        self.agentCount = agentCount
        self.statusCount = statusCount
        self.empty = empty
        self.steps = steps
        self.toolCalls = toolCalls
        self.started = started
        self.ran = ran
        self.actionsFor = actionsFor
        self.stop = stop
        self.open = open
        self.respond = respond
        self.openPart = openPart
        self.announce = announce
        self.expand = expand
        self.collapse = collapse
    }
}
