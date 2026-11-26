package com.rookhq.monarch.components.intelligence.aisharedsession

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchAiSharedSession`. Every
 * `null` keeps the built-in string `monarch_aisharedsession_<key>`.
 *
 * @property label names the bar for TalkBack, "Shared session".
 * @property listTitle the title of the participant list, "In this session".
 * @property people the heading of the people in the list, "People".
 * @property agents the heading of the agents in the list, "Agents".
 * @property agentKind said for agents where a person has a role, "AI agent".
 * @property assistant used when the answering agent is not a participant, "Assistant".
 * @property someone used when a requester is not a participant, "Someone".
 * @property quiet the status line when nothing is happening, "No requests in progress".
 * @property queueTitle names the queue for TalkBack, "Waiting requests".
 * @property withdraw the button on your own request, "Withdraw".
 * @property remove the owner's button on other people's requests, "Remove".
 * @property moveUp the owner's button that moves a request up, "Move up".
 * @property moveDown the owner's button that moves a request down, "Move down".
 * @property invite the invite button, "Invite".
 * @property follow the Follow toggle, "Follow".
 * @property viewerNotice the notice shown to viewers, "You can watch but not send".
 * @property requestAccess the viewer's button, "Request access".
 * @property accessRequested shown instead of the button once access was requested, "Access requested".
 * @property done closes the participant list, "Done".
 * @property here names the avatar stack from the number of people and agents, "3 people and 1 agent here".
 * @property showAll what the stack does, from the number of participants, "Show all 5 participants".
 * @property several stands in for the names when more than two people share an activity, "3 people".
 * @property queueCount the status clause of the queue, "2 requests waiting".
 * @property queueToggle the button that shows or hides the queue, "Queue (2)".
 * @property nameWithYou marks your own name, "Maya (you)".
 * @property followLabel names a Follow toggle for TalkBack, "Follow Sam Lee".
 * @property role the name of a role: "Owner", "Editor", "Viewer".
 * @property presence the word of a presence: "Active", "Idle", "Away".
 * @property activity the word of an activity: "Typing", "Speaking", "Reviewing".
 * @property activityLine the status clause of an activity, from the names (one or two names, or [several])
 * and their number, "Sam Lee is typing…".
 * @property answering the status clause while an agent answers someone else, from the agent, the requester and
 * the optional label, "Research agent is answering Sam’s request".
 * @property answeringYou the status clause while an agent answers you, from the agent and the optional label.
 * @property requestAction names a queue button for TalkBack, from its text, the requester and the position,
 * "Move up: request 2, from Sam Lee".
 * @property moved announced after a request moves, from the requester, its new position and the total.
 */
@Immutable
public class MonarchAiSharedSessionLabels(
    public val label: String? = null,
    public val listTitle: String? = null,
    public val people: String? = null,
    public val agents: String? = null,
    public val agentKind: String? = null,
    public val assistant: String? = null,
    public val someone: String? = null,
    public val quiet: String? = null,
    public val queueTitle: String? = null,
    public val withdraw: String? = null,
    public val remove: String? = null,
    public val moveUp: String? = null,
    public val moveDown: String? = null,
    public val invite: String? = null,
    public val follow: String? = null,
    public val viewerNotice: String? = null,
    public val requestAccess: String? = null,
    public val accessRequested: String? = null,
    public val done: String? = null,
    public val here: ((people: Int, agents: Int) -> String)? = null,
    public val showAll: ((total: Int) -> String)? = null,
    public val several: ((count: Int) -> String)? = null,
    public val queueCount: ((count: Int) -> String)? = null,
    public val queueToggle: ((count: Int) -> String)? = null,
    public val nameWithYou: ((name: String) -> String)? = null,
    public val followLabel: ((name: String) -> String)? = null,
    public val role: ((MonarchAiSharedSessionRole) -> String)? = null,
    public val presence: ((MonarchAiSharedSessionPresence) -> String)? = null,
    public val activity: ((MonarchAiSharedSessionActivity) -> String)? = null,
    public val activityLine: ((activity: MonarchAiSharedSessionActivity, names: String, count: Int) -> String)? = null,
    public val answering: ((agent: String, requester: String, label: String?) -> String)? = null,
    public val answeringYou: ((agent: String, label: String?) -> String)? = null,
    public val requestAction: ((action: String, requester: String, position: Int) -> String)? = null,
    public val moved: ((requester: String, position: Int, total: Int) -> String)? = null,
)
