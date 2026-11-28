/// Replacements for the built-in strings of ``MonarchAiSharedSession``.
///
/// Every property is `nil` by default, which keeps the built-in string.
@_spi(Experimental) public struct MonarchAiSharedSessionLabels: Sendable {
    /// Names the whole bar. Built in: "Shared session".
    public var label: String?
    /// Names the avatar stack from the number of people and agents. Built in: "3 people and 1 agent here".
    public var here: (@Sendable (Int, Int) -> String)?
    /// The hint of the stack, from the number of participants. Built in: "Shows all 4 participants."
    public var showAll: (@Sendable (Int) -> String)?
    /// The title of the list of participants. Built in: "In this session".
    public var listTitle: String?
    /// The heading of the people. Built in: "People".
    public var people: String?
    /// The heading of the agents. Built in: "Agents".
    public var agents: String?
    /// Marks your own name. Built in: "Maya (you)".
    public var nameWithYou: (@Sendable (String) -> String)?
    /// Said for agents where a person has a role. Built in: "AI agent".
    public var agentKind: String?
    /// The name of a role. Built in: "Owner", "Editor", "Viewer".
    public var role: (@Sendable (MonarchAiSharedSessionRole) -> String)?
    /// The word of a presence. Built in: "Active", "Idle", "Away".
    public var presence: (@Sendable (MonarchAiSharedSessionPresence) -> String)?
    /// The word of an activity. Built in: "Typing", "Speaking", "Reviewing".
    public var activity: (@Sendable (MonarchAiSharedSessionActivity) -> String)?
    /// The status clause of an activity, from the names and how many. Built in: "Maya is typing…", "Maya and Sam are reviewing".
    public var activityLine: (@Sendable (MonarchAiSharedSessionActivity, String, Int) -> String)?
    /// Stands in for the names when more than two share an activity. Built in: "3 people".
    public var several: (@Sendable (Int) -> String)?
    /// The status clause while an agent answers someone else, from the agent, the requester and the request's label. Built in: "Assistant is answering Sam’s request: Summarise the notes".
    public var answering: (@Sendable (String, String, String?) -> String)?
    /// The status clause while an agent answers you. Built in: "Assistant is answering your request".
    public var answeringYou: (@Sendable (String, String?) -> String)?
    /// Used when the answering agent is not a participant. Built in: "Assistant".
    public var assistant: String?
    /// Used when a requester is not a participant. Built in: "Someone".
    public var someone: String?
    /// The status clause of the queue. Built in: "2 requests waiting".
    public var queueCount: (@Sendable (Int) -> String)?
    /// The status line when nothing is happening. Built in: "No requests in progress".
    public var quiet: String?
    /// The button that shows or hides the queue. Built in: "Queue (2)".
    public var queueToggle: (@Sendable (Int) -> String)?
    /// Names the queue. Built in: "Waiting requests".
    public var queueTitle: String?
    /// Withdraws your own request. Built in: "Withdraw".
    public var withdraw: String?
    /// Removes someone's request, for owners. Built in: "Remove".
    public var remove: String?
    /// Moves a request up, for owners. Built in: "Move up".
    public var moveUp: String?
    /// Moves a request down, for owners. Built in: "Move down".
    public var moveDown: String?
    /// The spoken name of a queue button, from the action, the requester and the position. Built in: "Move up: request 2, from Sam".
    public var requestAction: (@Sendable (String, String, Int) -> String)?
    /// Announced after a move, from the requester, the new position and the total. Built in: "Sam’s request moved to position 1 of 3".
    public var moved: (@Sendable (String, Int, Int) -> String)?
    /// The invite button. Built in: "Invite".
    public var invite: String?
    /// The follow toggle. Built in: "Follow".
    public var follow: String?
    /// The spoken name of a follow toggle. Built in: "Follow Maya".
    public var followLabel: (@Sendable (String) -> String)?
    /// The notice shown to viewers. Built in: "You can watch but not send".
    public var viewerNotice: String?
    /// The request access button. Built in: "Request access".
    public var requestAccess: String?
    /// Shown once access was requested. Built in: "Access requested".
    public var accessRequested: String?
    /// Closes the list of participants. Built in: "Done".
    public var done: String?

    /// Replacements for built-in strings. Pass only the ones to replace.
    ///
    /// - Parameters:
    ///   - label: Names the whole bar.
    ///   - here: Names the avatar stack from the number of people and agents.
    ///   - showAll: The hint of the stack, from the number of participants.
    ///   - listTitle: The title of the list of participants.
    ///   - people: The heading of the people.
    ///   - agents: The heading of the agents.
    ///   - nameWithYou: Marks your own name.
    ///   - agentKind: Said for agents where a person has a role.
    ///   - role: The name of a role.
    ///   - presence: The word of a presence.
    ///   - activity: The word of an activity.
    ///   - activityLine: The status clause of an activity, from the names and how many.
    ///   - several: Stands in for the names when more than two share an activity.
    ///   - answering: The status clause while an agent answers someone else, from the agent, the requester and the request's label.
    ///   - answeringYou: The status clause while an agent answers you.
    ///   - assistant: Used when the answering agent is not a participant.
    ///   - someone: Used when a requester is not a participant.
    ///   - queueCount: The status clause of the queue.
    ///   - quiet: The status line when nothing is happening.
    ///   - queueToggle: The button that shows or hides the queue.
    ///   - queueTitle: Names the queue.
    ///   - withdraw: Withdraws your own request.
    ///   - remove: Removes someone's request, for owners.
    ///   - moveUp: Moves a request up, for owners.
    ///   - moveDown: Moves a request down, for owners.
    ///   - requestAction: The spoken name of a queue button, from the action, the requester and the position.
    ///   - moved: Announced after a move, from the requester, the new position and the total.
    ///   - invite: The invite button.
    ///   - follow: The follow toggle.
    ///   - followLabel: The spoken name of a follow toggle.
    ///   - viewerNotice: The notice shown to viewers.
    ///   - requestAccess: The request access button.
    ///   - accessRequested: Shown once access was requested.
    ///   - done: Closes the list of participants.
    public init(
        label: String? = nil,
        here: (@Sendable (Int, Int) -> String)? = nil,
        showAll: (@Sendable (Int) -> String)? = nil,
        listTitle: String? = nil,
        people: String? = nil,
        agents: String? = nil,
        nameWithYou: (@Sendable (String) -> String)? = nil,
        agentKind: String? = nil,
        role: (@Sendable (MonarchAiSharedSessionRole) -> String)? = nil,
        presence: (@Sendable (MonarchAiSharedSessionPresence) -> String)? = nil,
        activity: (@Sendable (MonarchAiSharedSessionActivity) -> String)? = nil,
        activityLine: (@Sendable (MonarchAiSharedSessionActivity, String, Int) -> String)? = nil,
        several: (@Sendable (Int) -> String)? = nil,
        answering: (@Sendable (String, String, String?) -> String)? = nil,
        answeringYou: (@Sendable (String, String?) -> String)? = nil,
        assistant: String? = nil,
        someone: String? = nil,
        queueCount: (@Sendable (Int) -> String)? = nil,
        quiet: String? = nil,
        queueToggle: (@Sendable (Int) -> String)? = nil,
        queueTitle: String? = nil,
        withdraw: String? = nil,
        remove: String? = nil,
        moveUp: String? = nil,
        moveDown: String? = nil,
        requestAction: (@Sendable (String, String, Int) -> String)? = nil,
        moved: (@Sendable (String, Int, Int) -> String)? = nil,
        invite: String? = nil,
        follow: String? = nil,
        followLabel: (@Sendable (String) -> String)? = nil,
        viewerNotice: String? = nil,
        requestAccess: String? = nil,
        accessRequested: String? = nil,
        done: String? = nil
    ) {
        self.label = label
        self.here = here
        self.showAll = showAll
        self.listTitle = listTitle
        self.people = people
        self.agents = agents
        self.nameWithYou = nameWithYou
        self.agentKind = agentKind
        self.role = role
        self.presence = presence
        self.activity = activity
        self.activityLine = activityLine
        self.several = several
        self.answering = answering
        self.answeringYou = answeringYou
        self.assistant = assistant
        self.someone = someone
        self.queueCount = queueCount
        self.quiet = quiet
        self.queueToggle = queueToggle
        self.queueTitle = queueTitle
        self.withdraw = withdraw
        self.remove = remove
        self.moveUp = moveUp
        self.moveDown = moveDown
        self.requestAction = requestAction
        self.moved = moved
        self.invite = invite
        self.follow = follow
        self.followLabel = followLabel
        self.viewerNotice = viewerNotice
        self.requestAccess = requestAccess
        self.accessRequested = accessRequested
        self.done = done
    }
}
