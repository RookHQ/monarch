package com.rookhq.monarch.components.intelligence.aiknowledgescope

import androidx.compose.ui.state.ToggleableState

/** One visible row of the tree: a source and its depth, starting at 1. */
internal class AiKnowledgeScopeRow(val source: MonarchAiKnowledgeSource, val level: Int)

/** The selection rules of MonarchAiKnowledgeScope, as on the web. Pure, so they are tested directly. */
internal object MonarchAiKnowledgeScopeLogic {
    /**
     * The check state of every source. A source is on when its id is
     * selected, when a group around it is, or when all of its children are on;
     * a group is mixed when only some of its contents are.
     */
    fun states(sources: List<MonarchAiKnowledgeSource>, selection: List<String>): Map<String, ToggleableState> {
        val selected = selection.toSet()
        val out = LinkedHashMap<String, ToggleableState>()
        fun visit(node: MonarchAiKnowledgeSource, inherited: Boolean): ToggleableState {
            val self = inherited || node.id in selected
            val state = if (!node.isGroup) {
                if (self) ToggleableState.On else ToggleableState.Off
            } else {
                val children = node.children.map { visit(it, self) }
                when {
                    self || children.all { it == ToggleableState.On } -> ToggleableState.On
                    children.all { it == ToggleableState.Off } -> ToggleableState.Off
                    else -> ToggleableState.Indeterminate
                }
            }
            out[node.id] = state
            return state
        }
        sources.forEach { visit(it, inherited = false) }
        return out
    }

    /**
     * The selection after the person toggles [id]: an unchecked source is
     * checked with everything in it, a checked or mixed one is cleared with
     * everything in it. The result lists every fully checked source, groups
     * included, in tree order; selected ids that are not in the tree are kept
     * at the end.
     */
    fun toggle(sources: List<MonarchAiKnowledgeSource>, selection: List<String>, id: String): List<String> {
        val states = states(sources, selection)
        val target = find(sources, id) ?: return selection
        val checked = states.filterValues { it == ToggleableState.On }.keys.toMutableSet()
        val turnOn = states[id] == ToggleableState.Off
        fun mark(node: MonarchAiKnowledgeSource) {
            if (turnOn) checked.add(node.id) else checked.remove(node.id)
            node.children.forEach(::mark)
        }
        mark(target)
        fun settle(node: MonarchAiKnowledgeSource): Boolean {
            if (!node.isGroup) return node.id in checked
            val all = node.children.map(::settle).all { it }
            if (all) checked.add(node.id) else checked.remove(node.id)
            return all
        }
        sources.forEach { settle(it) }
        val result = mutableListOf<String>()
        fun collect(node: MonarchAiKnowledgeSource) {
            if (node.id in checked) result.add(node.id)
            node.children.forEach(::collect)
        }
        sources.forEach(::collect)
        selection.filter { it !in states && it !in result }.forEach(result::add)
        return result
    }

    /** The number of selected sources, a fully selected group counting as one. */
    fun count(sources: List<MonarchAiKnowledgeSource>, states: Map<String, ToggleableState>): Int {
        var count = 0
        fun walk(node: MonarchAiKnowledgeSource) {
            when (states[node.id]) {
                ToggleableState.On -> count += 1
                ToggleableState.Indeterminate -> node.children.forEach(::walk)
                else -> Unit
            }
        }
        sources.forEach(::walk)
        return count
    }

    /** The visible rows: every source, and the contents of each open group after it. */
    fun rows(sources: List<MonarchAiKnowledgeSource>, expanded: Collection<String>): List<AiKnowledgeScopeRow> {
        val rows = mutableListOf<AiKnowledgeScopeRow>()
        fun walk(nodes: List<MonarchAiKnowledgeSource>, level: Int) {
            nodes.forEach { node ->
                rows.add(AiKnowledgeScopeRow(node, level))
                if (node.isGroup && node.id in expanded) walk(node.children, level + 1)
            }
        }
        walk(sources, 1)
        return rows
    }

    /** [expanded] with [id] added or removed. */
    fun toggleExpanded(expanded: List<String>, id: String): List<String> =
        if (id in expanded) expanded - id else expanded + id

    private fun find(nodes: List<MonarchAiKnowledgeSource>, id: String): MonarchAiKnowledgeSource? {
        nodes.forEach { node ->
            if (node.id == id) return node
            find(node.children, id)?.let { return it }
        }
        return null
    }
}
