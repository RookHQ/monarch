package dev.monarchui.components.intelligence.aisharedsession

import android.content.res.Resources
import dev.monarchui.R

/** Every string of the bar, from the labels or the built-in resources. One function per string. */
@Suppress("TooManyFunctions")
internal class MonarchAiSharedSessionTexts(
    private val resources: Resources,
    private val labels: MonarchAiSharedSessionLabels,
) {
    private fun string(override: String?, id: Int): String = override ?: resources.getString(id)

    val label: String get() = string(labels.label, R.string.monarch_aisharedsession_label)
    val listTitle: String get() = string(labels.listTitle, R.string.monarch_aisharedsession_list_title)
    val people: String get() = string(labels.people, R.string.monarch_aisharedsession_people)
    val agents: String get() = string(labels.agents, R.string.monarch_aisharedsession_agents)
    val agentKind: String get() = string(labels.agentKind, R.string.monarch_aisharedsession_agent_kind)
    val someone: String get() = string(labels.someone, R.string.monarch_aisharedsession_someone)
    val quiet: String get() = string(labels.quiet, R.string.monarch_aisharedsession_quiet)
    val queueTitle: String get() = string(labels.queueTitle, R.string.monarch_aisharedsession_queue_title)
    val withdraw: String get() = string(labels.withdraw, R.string.monarch_aisharedsession_withdraw)
    val remove: String get() = string(labels.remove, R.string.monarch_aisharedsession_remove)
    val moveUp: String get() = string(labels.moveUp, R.string.monarch_aisharedsession_move_up)
    val moveDown: String get() = string(labels.moveDown, R.string.monarch_aisharedsession_move_down)
    val invite: String get() = string(labels.invite, R.string.monarch_aisharedsession_invite)
    val follow: String get() = string(labels.follow, R.string.monarch_aisharedsession_follow)
    val viewerNotice: String get() = string(labels.viewerNotice, R.string.monarch_aisharedsession_viewer_notice)
    val requestAccess: String get() = string(labels.requestAccess, R.string.monarch_aisharedsession_request_access)
    val accessRequested: String
        get() = string(labels.accessRequested, R.string.monarch_aisharedsession_access_requested)
    val done: String get() = string(labels.done, R.string.monarch_aisharedsession_done)
    val separator: String get() = resources.getString(R.string.monarch_aisharedsession_separator)
    val announceSeparator: String get() = resources.getString(R.string.monarch_aisharedsession_announce_separator)
    val detailsSeparator: String get() = resources.getString(R.string.monarch_aisharedsession_details_separator)
    val facesSeparator: String get() = resources.getString(R.string.monarch_aisharedsession_faces_separator)

    fun here(people: Int, agents: Int): String = labels.here?.invoke(people, agents) ?: run {
        val parts = listOfNotNull(
            count(people, R.plurals.monarch_aisharedsession_people_count),
            count(agents, R.plurals.monarch_aisharedsession_agents_count),
        )
        if (parts.size == 2) {
            resources.getString(R.string.monarch_aisharedsession_here_both, parts[0], parts[1])
        } else {
            resources.getString(R.string.monarch_aisharedsession_here, parts.firstOrNull().orEmpty())
        }
    }

    fun showAll(total: Int): String = labels.showAll?.invoke(total)
        ?: resources.getQuantityString(R.plurals.monarch_aisharedsession_show_all, total, total)

    fun queueToggle(count: Int): String = labels.queueToggle?.invoke(count)
        ?: resources.getString(R.string.monarch_aisharedsession_queue_toggle, count)

    fun nameWithYou(name: String): String = labels.nameWithYou?.invoke(name)
        ?: resources.getString(R.string.monarch_aisharedsession_name_with_you, name)

    fun followLabel(name: String): String = labels.followLabel?.invoke(name)
        ?: resources.getString(R.string.monarch_aisharedsession_follow_label, name)

    fun role(role: MonarchAiSharedSessionRole): String = labels.role?.invoke(role) ?: resources.getString(
        when (role) {
            MonarchAiSharedSessionRole.Owner -> R.string.monarch_aisharedsession_role_owner
            MonarchAiSharedSessionRole.Editor -> R.string.monarch_aisharedsession_role_editor
            MonarchAiSharedSessionRole.Viewer -> R.string.monarch_aisharedsession_role_viewer
        },
    )

    fun presence(presence: MonarchAiSharedSessionPresence): String = labels.presence?.invoke(presence)
        ?: resources.getString(
            when (presence) {
                MonarchAiSharedSessionPresence.Active -> R.string.monarch_aisharedsession_presence_active
                MonarchAiSharedSessionPresence.Idle -> R.string.monarch_aisharedsession_presence_idle
                MonarchAiSharedSessionPresence.Away -> R.string.monarch_aisharedsession_presence_away
            },
        )

    fun activity(activity: MonarchAiSharedSessionActivity): String = labels.activity?.invoke(activity)
        ?: resources.getString(
            when (activity) {
                MonarchAiSharedSessionActivity.Typing -> R.string.monarch_aisharedsession_activity_typing
                MonarchAiSharedSessionActivity.Speaking -> R.string.monarch_aisharedsession_activity_speaking
                MonarchAiSharedSessionActivity.Reviewing -> R.string.monarch_aisharedsession_activity_reviewing
            },
        )

    fun requestAction(action: String, requester: String, position: Int): String =
        labels.requestAction?.invoke(action, requester, position)
            ?: resources.getString(R.string.monarch_aisharedsession_request_action, action, position, requester)

    fun moved(requester: String, position: Int, total: Int): String = labels.moved?.invoke(requester, position, total)
        ?: resources.getString(R.string.monarch_aisharedsession_moved, requester, position, total)

    private fun withLabel(text: String, label: String?): String =
        if (label == null) text else resources.getString(R.string.monarch_aisharedsession_with_label, text, label)

    /** The texts of the status line. */
    fun status(): MonarchAiSharedSessionStatusTexts = MonarchAiSharedSessionStatusTexts(
        answering = { agent, requester, label ->
            labels.answering?.invoke(agent, requester, label)
                ?: withLabel(resources.getString(R.string.monarch_aisharedsession_answering, agent, requester), label)
        },
        answeringYou = { agent, label ->
            labels.answeringYou?.invoke(agent, label)
                ?: withLabel(resources.getString(R.string.monarch_aisharedsession_answering_you, agent), label)
        },
        activityLine = { activity, names, count ->
            labels.activityLine?.invoke(activity, names, count)
                ?: resources.getString(activityLine(activity, count), names)
        },
        twoNames = { first, second -> resources.getString(R.string.monarch_aisharedsession_two_names, first, second) },
        several = { count ->
            labels.several?.invoke(count)
                ?: resources.getQuantityString(R.plurals.monarch_aisharedsession_several, count, count)
        },
        queueCount = { count ->
            labels.queueCount?.invoke(count)
                ?: resources.getQuantityString(R.plurals.monarch_aisharedsession_queue_count, count, count)
        },
        assistant = string(labels.assistant, R.string.monarch_aisharedsession_assistant),
        someone = someone,
    )

    private fun activityLine(activity: MonarchAiSharedSessionActivity, count: Int): Int = when (activity) {
        MonarchAiSharedSessionActivity.Typing ->
            pick(count, R.string.monarch_aisharedsession_typing_one, R.string.monarch_aisharedsession_typing_many)
        MonarchAiSharedSessionActivity.Speaking ->
            pick(count, R.string.monarch_aisharedsession_speaking_one, R.string.monarch_aisharedsession_speaking_many)
        MonarchAiSharedSessionActivity.Reviewing ->
            pick(count, R.string.monarch_aisharedsession_reviewing_one, R.string.monarch_aisharedsession_reviewing_many)
    }

    private fun count(value: Int, id: Int): String? =
        if (value > 0) resources.getQuantityString(id, value, value) else null

    private fun pick(count: Int, one: Int, many: Int): Int = if (count == 1) one else many

    /** "Sam Lee, Editor, Active, Typing": the name and the details, for TalkBack and the list. */
    fun details(participant: MonarchAiSharedSessionParticipant, doing: String?): List<String> = listOfNotNull(
        if (participant.kind == MonarchAiSharedSessionKind.Agent) agentKind else role(participant.role),
        presence(participant.presence),
        doing,
    )

    /** The display name: "Maya (you)" for you. */
    fun displayName(participant: MonarchAiSharedSessionParticipant): String =
        if (participant.you) nameWithYou(participant.name) else participant.name
}
