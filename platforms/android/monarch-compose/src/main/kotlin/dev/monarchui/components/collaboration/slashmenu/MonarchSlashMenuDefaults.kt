package dev.monarchui.components.collaboration.slashmenu

/** The filter and grouping a `MonarchSlashMenu` uses, for apps that need them elsewhere. */
public object MonarchSlashMenuDefaults {
    private const val RANK_START = 0
    private const val RANK_LABEL = 1
    private const val RANK_OTHER = 2

    /**
     * [items] that match [query], ranked as the web's filterSlashMenuItems:
     * labels that start with the query first, then labels that contain it,
     * then matches in the id, description or keywords; ties keep their order.
     * Case is ignored; an empty query keeps every item.
     */
    public fun filter(items: List<MonarchSlashMenuItem>, query: String): List<MonarchSlashMenuItem> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return items
        return items
            .mapNotNull { item ->
                val label = item.label.lowercase()
                val rest = (listOf(item.id, item.description.orEmpty()) + item.keywords).joinToString(" ").lowercase()
                val rank = when {
                    label.startsWith(q) -> RANK_START
                    q in label -> RANK_LABEL
                    q in rest -> RANK_OTHER
                    else -> null
                }
                rank?.let { it to item }
            }
            .sortedBy { it.first }
            .map { it.second }
    }

    /**
     * [items] split into groups in display order: the groups named in
     * [groupOrder] first, in that order, then the others in the order they
     * first appear. Empty groups are dropped; the heading of items without a
     * group is `null`.
     */
    public fun group(
        items: List<MonarchSlashMenuItem>,
        groupOrder: List<String> = emptyList(),
    ): List<Pair<String?, List<MonarchSlashMenuItem>>> {
        val groups = LinkedHashMap<String?, MutableList<MonarchSlashMenuItem>>()
        groupOrder.forEach { groups[it] = mutableListOf() }
        items.forEach { groups.getOrPut(it.group) { mutableListOf() }.add(it) }
        return groups.filterValues { it.isNotEmpty() }.map { (heading, list) -> heading to list.toList() }
    }
}
