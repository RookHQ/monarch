package dev.monarchui.components.workflow.backlogplanner


private typealias MovesTokens = MonarchBacklogPlannerTokens

/** The pure moving rules of MonarchBacklogPlanner: finding, moving and stepping items, and drag targets. */
internal object MonarchBacklogPlannerMoves {
    /** A place in the planner: a bucket and an index in it. */
    data class Location(val bucketId: String, val index: Int)

    /** Where an item is: its bucket's position in the list, its index in the bucket and the item. */
    data class Found<T>(val bucket: Int, val index: Int, val item: T)

    /** Finds the item with [key], or `null`. */
    fun <T> locate(buckets: List<MonarchBacklogBucket<T>>, key: String, keyOf: (T) -> String): Found<T>? =
        buckets.withIndex().firstNotNullOfOrNull { (b, bucket) ->
            bucket.items.indexOfFirst { keyOf(it) == key }.takeIf { it != -1 }?.let { Found(b, it, bucket.items[it]) }
        }

    /** Where the item with [key] is, as a location, or `null`. */
    fun <T> where(buckets: List<MonarchBacklogBucket<T>>, key: String, keyOf: (T) -> String): Location? =
        locate(buckets, key, keyOf)?.let { Location(buckets[it.bucket].id, it.index) }

    /** A copy of [buckets] with the item [key] at [to], clamped. Unknown items or buckets leave it unchanged. */
    fun <T> move(
        buckets: List<MonarchBacklogBucket<T>>,
        key: String,
        to: Location,
        keyOf: (T) -> String,
    ): List<MonarchBacklogBucket<T>> {
        val from = locate(buckets, key, keyOf)
        if (from == null || buckets.none { it.id == to.bucketId }) return buckets
        val source = buckets[from.bucket].id
        return buckets.map { bucket ->
            if (bucket.id != source && bucket.id != to.bucketId) return@map bucket
            val items = bucket.items.toMutableList()
            if (bucket.id == source) items.removeAt(from.index)
            if (bucket.id == to.bucketId) items.add(to.index.coerceIn(0, items.size), from.item)
            bucket.withItems(items)
        }
    }

    /**
     * The next place up ([direction] -1) or down (+1) for the item [key],
     * crossing into the next bucket that is not [blocked]: the end of the one
     * above or the start of the one below. `null` when there is none.
     */
    fun <T> step(
        buckets: List<MonarchBacklogBucket<T>>,
        key: String,
        direction: Int,
        keyOf: (T) -> String,
        blocked: (String) -> Boolean,
    ): Location? {
        val at = locate(buckets, key, keyOf) ?: return null
        val bucket = buckets[at.bucket]
        val within = at.index + direction
        if (within in bucket.items.indices) return Location(bucket.id, within)
        val others = if (direction > 0) buckets.drop(at.bucket + 1) else buckets.take(at.bucket).reversed()
        return others.firstOrNull { !blocked(it.id) }
            ?.let { Location(it.id, if (direction > 0) 0 else it.items.size) }
    }

    /** The keys of the lazy list in order: each header, then the rows, empty text and footer of open buckets. */
    fun <T> entries(
        buckets: List<MonarchBacklogBucket<T>>,
        collapsed: Set<String>,
        keyOf: (T) -> String,
    ): List<String> = buckets.flatMap { bucket ->
        buildList {
            add(MovesTokens.headerKey(bucket.id))
            if (bucket.id !in collapsed) {
                bucket.items.forEach { add(MovesTokens.itemKey(keyOf(it))) }
                if (bucket.items.isEmpty()) add(MovesTokens.emptyKey(bucket.id))
                if (bucket.footer != null) add(MovesTokens.footerKey(bucket.id))
            }
        }
    }

    /**
     * Where the dragged item [key] goes when its middle is over the lazy
     * entry [slot]: the place of a row, the start of an empty bucket, the
     * start of the bucket below a header it moves down past, or the end of the
     * bucket above a header it moves up past. `null` when the place is
     * [blocked] or the item stays.
     */
    fun <T> target(
        slot: String,
        buckets: List<MonarchBacklogBucket<T>>,
        key: String,
        keyOf: (T) -> String,
        blocked: (String) -> Boolean,
    ): Location? {
        val at = locate(buckets, key, keyOf) ?: return null
        val id = slot.drop(2)
        val place = when (slot.take(2)) {
            "i:" -> where(buckets, id, keyOf)
            "e:" -> Location(id, 0)
            "h:" -> headerTarget(buckets, buckets.indexOfFirst { it.id == id }, at.bucket)
            "f:" -> buckets.indexOfFirst { it.id == id }.takeIf { it in 0 until at.bucket }
                ?.let { Location(buckets[it].id, buckets[it].items.size) }
            else -> null
        }
        return place?.takeUnless { blocked(it.bucketId) || it == Location(buckets[at.bucket].id, at.index) }
    }

    /** Past the header of bucket [b]: its start when moving down from bucket [from], else the end of the one above. */
    private fun <T> headerTarget(buckets: List<MonarchBacklogBucket<T>>, b: Int, from: Int): Location? = when {
        b < 0 -> null
        from < b -> Location(buckets[b].id, 0)
        b > 0 -> buckets[b - 1].let { above -> Location(above.id, above.items.size) }
        else -> null
    }

    /** The total of the known points, or `null` without a points function. */
    fun <T> points(items: List<T>, pointsOf: ((T) -> Int?)?): Int? =
        pointsOf?.let { f -> items.sumOf { f(it) ?: 0 } }

    /** The number of items without an estimate. */
    fun <T> unestimated(items: List<T>, pointsOf: ((T) -> Int?)?): Int =
        pointsOf?.let { f -> items.count { f(it) == null } } ?: 0

}
