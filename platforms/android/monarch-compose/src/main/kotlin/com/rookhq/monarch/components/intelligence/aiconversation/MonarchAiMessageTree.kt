package com.rookhq.monarch.components.intelligence.aiconversation

/**
 * The branching message tree behind `MonarchAiConversation`: every
 * regenerate or edit adds a sibling, and the visible conversation is one
 * path through it.
 */
internal class MonarchAiMessageTree(messages: List<MonarchAiConversationMessage> = emptyList()) {
    /** Every message by id. */
    private val nodes = mutableMapOf<String, MonarchAiConversationMessage>()

    /** The parent of each message. A message under the root has no entry. */
    private val parent = mutableMapOf<String, String>()

    /** The children of each parent id, in the order they were added. */
    private val children = mutableMapOf<String, MutableList<String>>()

    /** The index of the shown child of each parent id. */
    private val active = mutableMapOf<String, Int>()

    init {
        var previous = ROOT
        messages.forEach { message ->
            // A stored conversation is one branch; positions are worked out from the tree.
            addChild(message.copy(branch = null), previous)
            previous = message.id
        }
    }

    /** The message with this id, as stored. */
    operator fun get(id: String): MonarchAiConversationMessage? = nodes[id]

    /** The parent id of a message, or the root. */
    fun parentId(of: String): String = parent[of] ?: ROOT

    /** Adds a message under a parent and shows it. */
    fun addChild(message: MonarchAiConversationMessage, under: String) {
        nodes[message.id] = message
        if (under != ROOT) parent[message.id] = under
        val siblings = children.getOrPut(under) { mutableListOf() }
        siblings += message.id
        active[under] = siblings.size - 1
    }

    /** Replaces one message. Does nothing when the message is not in the tree. */
    fun replace(message: MonarchAiConversationMessage) {
        if (nodes.containsKey(message.id)) nodes[message.id] = message
    }

    /** Shows another alternative of a message. Returns whether the index was valid. */
    fun activate(index: Int, siblingsOf: String): Boolean {
        val parentId = parentId(siblingsOf)
        val count = children[parentId]?.size ?: 0
        if (!nodes.containsKey(siblingsOf) || index !in 0 until count) return false
        active[parentId] = index
        return true
    }

    /** The ids on the visible path, oldest first. */
    val visibleIds: List<String>
        get() = generateSequence(shownChild(ROOT)) { shownChild(it) }.toList()

    /** The shown child of a parent id, or `null` when it has none. */
    private fun shownChild(parentId: String): String? {
        val siblings = children[parentId].orEmpty()
        if (siblings.isEmpty()) return null
        return siblings[(active[parentId] ?: 0).coerceIn(0, siblings.size - 1)].takeIf { nodes.containsKey(it) }
    }

    /** The visible conversation: the active child at every level, oldest first. Alternatives carry `branch`. */
    val visiblePath: List<MonarchAiConversationMessage>
        get() = visibleIds.mapNotNull { id ->
            val message = nodes[id] ?: return@mapNotNull null
            val siblings = children[parentId(id)].orEmpty()
            val branch = if (siblings.size > 1) MonarchAiBranchPosition(siblings.indexOf(id), siblings.size) else null
            message.copy(branch = branch)
        }

    /** The last id on the visible path, or the root. */
    val lastVisibleId: String
        get() = visibleIds.lastOrNull() ?: ROOT

    /** The ids of the ancestors of a message, nearest first. */
    fun ancestors(of: String): List<String> = generateSequence(parent[of]) { parent[it] }.toList()

    companion object {
        /** The id of the root, which is the parent of the first messages. */
        const val ROOT: String = ""
    }
}
