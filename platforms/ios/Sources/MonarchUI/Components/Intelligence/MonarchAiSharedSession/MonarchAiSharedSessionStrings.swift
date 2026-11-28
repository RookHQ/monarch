/// Built-in strings of the shared session bar, from the `MonarchAiSharedSession` strings table.
enum MonarchAiSharedSessionStrings {
    private static let table = "MonarchAiSharedSession"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.aiSharedSession." + key, table: table)
    }

    private static func format(_ key: String, _ arguments: any CVarArg...) -> String {
        String(format: string(key), arguments: arguments)
    }

    private static func counted(_ key: String, _ count: Int) -> String {
        format(count == 1 ? key + "One" : key, count)
    }

    /// Names the whole bar.
    static var label: String { string("label") }
    /// The title of the list of participants.
    static var listTitle: String { string("listTitle") }
    /// The heading of the people.
    static var people: String { string("people") }
    /// The heading of the agents.
    static var agents: String { string("agents") }
    /// Said for agents where a person has a role.
    static var agentKind: String { string("agentKind") }
    /// Used when the answering agent is not a participant.
    static var assistant: String { string("assistant") }
    /// Used when a requester is not a participant.
    static var someone: String { string("someone") }
    /// The status line when nothing is happening.
    static var quiet: String { string("quiet") }
    /// Names the queue.
    static var queueTitle: String { string("queueTitle") }
    /// Withdraws your own request.
    static var withdraw: String { string("withdraw") }
    /// Removes someone's request.
    static var remove: String { string("remove") }
    /// Moves a request up.
    static var moveUp: String { string("moveUp") }
    /// Moves a request down.
    static var moveDown: String { string("moveDown") }
    /// The invite button.
    static var invite: String { string("invite") }
    /// The follow toggle.
    static var follow: String { string("follow") }
    /// The notice shown to viewers.
    static var viewerNotice: String { string("viewerNotice") }
    /// The request access button.
    static var requestAccess: String { string("requestAccess") }
    /// Shown once access was requested.
    static var accessRequested: String { string("accessRequested") }
    /// Closes the list of participants.
    static var done: String { string("done") }
    /// What separates the clauses of the status line.
    static var separator: String { string("separator") }

    /// The name of a role.
    static func role(_ role: MonarchAiSharedSessionRole) -> String { string("role." + role.rawValue) }
    /// The word of a presence.
    static func presence(_ presence: MonarchAiSharedSessionPresence) -> String {
        string("presence." + presence.rawValue)
    }
    /// The word of an activity.
    static func activity(_ activity: MonarchAiSharedSessionActivity) -> String {
        string("activity." + activity.rawValue)
    }
    /// "Maya is typing…" or "Maya and Sam are typing…".
    static func activityLine(_ activity: MonarchAiSharedSessionActivity, names: String, count: Int) -> String {
        format("activityLine." + activity.rawValue + (count == 1 ? "One" : ""), names)
    }
    /// "3 people".
    static func several(_ count: Int) -> String { counted("several", count) }
    /// "1 person", "3 people".
    static func peopleCount(_ count: Int) -> String { counted("peopleCount", count) }
    /// "1 agent", "2 agents".
    static func agentCount(_ count: Int) -> String { counted("agentCount", count) }
    /// "3 people and 1 agent here".
    static func here(people: Int, agents: Int) -> String {
        switch (people > 0, agents > 0) {
        case (true, true): format("hereBoth", peopleCount(people), agentCount(agents))
        case (true, false): format("here", peopleCount(people))
        case (false, true): format("here", agentCount(agents))
        case (false, false): format("here", peopleCount(0))
        }
    }
    /// "Shows all 4 participants."
    static func showAll(_ count: Int) -> String { counted("showAll", count) }
    /// "Maya (you)".
    static func nameWithYou(_ name: String) -> String { format("nameWithYou", name) }
    /// "Assistant is answering Sam’s request".
    static func answering(agent: String, requester: String) -> String { format("answering", agent, requester) }
    /// "Assistant is answering your request".
    static func answeringYou(agent: String) -> String { format("answeringYou", agent) }
    /// A clause with the request's label: "…request: Summarise the notes".
    static func withLabel(_ text: String, _ label: String) -> String { format("withLabel", text, label) }
    /// "2 requests waiting".
    static func queueCount(_ count: Int) -> String { counted("queueCount", count) }
    /// "Queue (2)".
    static func queueToggle(_ count: Int) -> String { format("queueToggle", count) }
    /// "Move up: request 2, from Sam".
    static func requestAction(_ action: String, requester: String, position: Int) -> String {
        format("requestAction", action, position, requester)
    }
    /// "Sam’s request moved to position 1 of 3".
    static func moved(_ requester: String, position: Int, total: Int) -> String {
        format("moved", requester, position, total)
    }
    /// "Follow Maya".
    static func followLabel(_ name: String) -> String { format("followLabel", name) }
}
