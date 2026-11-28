package dev.monarchui.components.intelligence.aisharedsession

import dev.monarchui.components.identity.presencedot.MonarchPresenceStatus

/** What a clause of the status line is about. */
internal enum class MonarchAiSharedSessionClauseKind { Answering, Typing, Speaking, Reviewing, Queue }

/** One clause of the status line, and whether it is worth announcing (typing and speaking are not). */
internal data class MonarchAiSharedSessionClause(
    val kind: MonarchAiSharedSessionClauseKind,
    val text: String,
    val announce: Boolean,
)

/** The texts the status line is made of, resolved by the caller so the logic stays pure. */
internal class MonarchAiSharedSessionStatusTexts(
    val answering: (agent: String, requester: String, label: String?) -> String,
    val answeringYou: (agent: String, label: String?) -> String,
    val activityLine: (activity: MonarchAiSharedSessionActivity, names: String, count: Int) -> String,
    val twoNames: (first: String, second: String) -> String,
    val several: (count: Int) -> String,
    val queueCount: (count: Int) -> String,
    val assistant: String,
    val someone: String,
)

/** People (you first) and agents, otherwise in the given order. */
internal class MonarchAiSharedSessionGroups(
    val people: List<MonarchAiSharedSessionParticipant>,
    val agents: List<MonarchAiSharedSessionParticipant>,
    val you: MonarchAiSharedSessionParticipant?,
)

/** The pure logic of MonarchAiSharedSession. */
internal object MonarchAiSharedSessionLogic {
    private const val MAX_NAMES = 2

    /** Splits the participants into people, with you first, and agents. */
    fun groups(participants: List<MonarchAiSharedSessionParticipant>): MonarchAiSharedSessionGroups {
        val people = participants.filter { it.kind == MonarchAiSharedSessionKind.Person }
        val agents = participants.filter { it.kind == MonarchAiSharedSessionKind.Agent }
        val you = people.firstOrNull { it.you }
        val ordered = if (you == null) people else listOf(you) + people.filter { it !== you }
        return MonarchAiSharedSessionGroups(ordered, agents, you)
    }

    /**
     * The clauses of the status line, most important first: who is being answered, what other people are
     * doing (your own activity is left out) and how many requests wait.
     */
    fun status(
        participants: List<MonarchAiSharedSessionParticipant>,
        answering: MonarchAiSharedSessionAnswering?,
        queueSize: Int,
        texts: MonarchAiSharedSessionStatusTexts,
    ): List<MonarchAiSharedSessionClause> {
        val byId = participants.associateBy { it.id }
        val clauses = ArrayList<MonarchAiSharedSessionClause>()
        if (answering != null) {
            val agent = byId[answering.participantId]?.name ?: texts.assistant
            val requester = byId[answering.forParticipantId]
            val label = answering.label?.takeIf { it.isNotBlank() }
            val text = if (requester?.you == true) {
                texts.answeringYou(agent, label)
            } else {
                texts.answering(agent, requester?.name ?: texts.someone, label)
            }
            clauses += MonarchAiSharedSessionClause(MonarchAiSharedSessionClauseKind.Answering, text, announce = true)
        }
        for (activity in MonarchAiSharedSessionActivity.entries) {
            val names = participants.filter { it.activity == activity && !it.you }.map { it.name }
            if (names.isEmpty()) continue
            val who = when {
                names.size > MAX_NAMES -> texts.several(names.size)
                names.size == MAX_NAMES -> texts.twoNames(names[0], names[1])
                else -> names[0]
            }
            clauses += MonarchAiSharedSessionClause(
                kind = clauseKind(activity),
                text = texts.activityLine(activity, who, names.size),
                announce = activity == MonarchAiSharedSessionActivity.Reviewing,
            )
        }
        if (queueSize > 0) {
            clauses += MonarchAiSharedSessionClause(
                MonarchAiSharedSessionClauseKind.Queue,
                texts.queueCount(queueSize),
                announce = true,
            )
        }
        return clauses
    }

    /** The visible status line: every clause joined. */
    fun line(clauses: List<MonarchAiSharedSessionClause>, separator: String): String =
        clauses.joinToString(separator) { it.text }

    /** What TalkBack is told: the clauses worth announcing only. */
    fun announcement(clauses: List<MonarchAiSharedSessionClause>, separator: String): String =
        clauses.filter { it.announce }.joinToString(separator) { it.text }

    /** The request ids after moving the request at [from] to [to]; `null` when the move is not possible. */
    fun reorder(ids: List<String>, from: Int, to: Int): List<String>? {
        if (from !in ids.indices || to !in ids.indices || from == to) return null
        return ids.toMutableList().apply { add(to, removeAt(from)) }
    }

    /** The presence dot of a participant: active is online, idle is away, away is offline. */
    fun presenceStatus(presence: MonarchAiSharedSessionPresence): MonarchPresenceStatus = when (presence) {
        MonarchAiSharedSessionPresence.Active -> MonarchPresenceStatus.Online
        MonarchAiSharedSessionPresence.Idle -> MonarchPresenceStatus.Away
        MonarchAiSharedSessionPresence.Away -> MonarchPresenceStatus.Offline
    }

    /** Whether the viewer may manage the queue. */
    fun owner(you: MonarchAiSharedSessionParticipant?): Boolean = you?.role == MonarchAiSharedSessionRole.Owner

    /** Whether the viewer only watches. */
    fun viewer(you: MonarchAiSharedSessionParticipant?): Boolean = you?.role == MonarchAiSharedSessionRole.Viewer

    private fun clauseKind(activity: MonarchAiSharedSessionActivity): MonarchAiSharedSessionClauseKind =
        when (activity) {
            MonarchAiSharedSessionActivity.Typing -> MonarchAiSharedSessionClauseKind.Typing
            MonarchAiSharedSessionActivity.Speaking -> MonarchAiSharedSessionClauseKind.Speaking
            MonarchAiSharedSessionActivity.Reviewing -> MonarchAiSharedSessionClauseKind.Reviewing
        }
}
