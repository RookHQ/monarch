package com.rookhq.monarch.components.workflow.orgchart

/**
 * The reporting structure of a `MonarchOrgChart`, built once per list of
 * nodes. A repeated id keeps its first node; a node whose manager is missing
 * or itself is top level; nodes reachable only through a cycle are not drawn.
 */
internal class MonarchOrgChartTree(nodes: List<MonarchOrgChartNode>) {
    val byId: Map<String, MonarchOrgChartNode> = nodes.distinctBy { it.id }.associateBy { it.id }
    val roots: List<MonarchOrgChartNode>
    private val children: Map<String, List<MonarchOrgChartNode>>

    init {
        val top = mutableListOf<MonarchOrgChartNode>()
        val kids = LinkedHashMap<String, MutableList<MonarchOrgChartNode>>()
        byId.values.forEach { node ->
            val parent = node.parentId?.takeIf { it != node.id && it in byId }
            if (parent == null) top += node else kids.getOrPut(parent) { mutableListOf() } += node
        }
        roots = top
        children = kids
    }

    /** The loaded direct reports of [id]. */
    fun reportsOf(id: String): List<MonarchOrgChartNode> = children[id].orEmpty()

    /** The number of direct reports: the larger of `childCount` and the loaded reports. */
    fun reportCount(node: MonarchOrgChartNode): Int = maxOf(node.childCount ?: 0, reportsOf(node.id).size)

    /** Whether opening [node]'s team needs `onLoadChildren` first. */
    fun needsLoad(node: MonarchOrgChartNode): Boolean = (node.childCount ?: 0) > reportsOf(node.id).size

    /** The managers above [id], nearest first. Stops at a repeat. */
    fun ancestors(id: String): List<String> {
        val out = mutableListOf<String>()
        val seen = mutableSetOf(id)
        var current = byId[id]?.parentId
        while (current != null && current in byId && seen.add(current)) {
            out += current
            current = byId[current]?.parentId?.takeIf { it != current }
        }
        return out
    }

    /**
     * The teams open at first: the top level, the level under it when there
     * are three or fewer top-level people, and the path to [focusId]. Teams
     * that still need loading start hidden.
     */
    fun initiallyExpanded(focusId: String?): Set<String> {
        val open = roots.filterNot(::needsLoad).mapTo(LinkedHashSet()) { it.id }
        if (roots.size <= SMALL_TOP_LEVEL) {
            roots.forEach { root -> reportsOf(root.id).filterNot(::needsLoad).forEach { open += it.id } }
        }
        focusId?.let { open += ancestors(it) }
        return open
    }

    /** The visible people, depth first, with their depth (0 at the top). */
    fun visible(expanded: Set<String>): List<Pair<MonarchOrgChartNode, Int>> {
        val out = mutableListOf<Pair<MonarchOrgChartNode, Int>>()
        fun visit(node: MonarchOrgChartNode, depth: Int, path: Set<String>) {
            out += node to depth
            if (node.id !in expanded) return
            val next = path + node.id
            reportsOf(node.id).forEach { if (it.id !in next) visit(it, depth + 1, next) }
        }
        roots.forEach { visit(it, 0, emptySet()) }
        return out
    }

    /**
     * Whether [personId] may move under [managerId]: both are drawn people
     * (not open positions), the manager is not the person, not their current
     * manager and not in their own team.
     */
    fun canMove(personId: String, managerId: String): Boolean {
        val person = byId[personId] ?: return false
        val manager = byId[managerId] ?: return false
        return person.kind == MonarchOrgChartNodeKind.Person &&
            manager.kind == MonarchOrgChartNodeKind.Person &&
            personId != managerId &&
            person.parentId != managerId &&
            personId !in ancestors(managerId)
    }

    /** Whether [personId] may move to the top level: a person who is not already there. */
    fun canMoveToTop(personId: String): Boolean {
        val person = byId[personId] ?: return false
        return person.kind == MonarchOrgChartNodeKind.Person && roots.none { it.id == personId }
    }

    /** The names of the secondary managers of every person, from [lines] whose ends are both in the chart. */
    fun dottedManagers(lines: List<MonarchOrgChartDottedLine>): Map<String, List<String>> =
        lines.filter { it.from in byId && it.to in byId && it.from != it.to }
            .groupBy({ it.from }, { byId.getValue(it.to).name })

    private companion object {
        const val SMALL_TOP_LEVEL = 3
    }
}
