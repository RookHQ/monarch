package com.rookhq.monarch.components.intelligence.airelatedentities

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** The ranking, relation and layout rules of MonarchAiRelatedEntities. Pure, so they are tested directly. */
internal object MonarchAiRelatedEntitiesLogic {
    /** Above this font scale the graph's one-line labels would overlap, so the list is shown. */
    const val GRAPH_MAX_FONT_SCALE: Float = 1.3f
    private const val PERCENT = 100

    /** A weight limited to 0 through 1, or `null` when there is none or it is not a number. */
    fun weight(weight: Double?): Double? = weight?.takeIf { it.isFinite() }?.coerceIn(0.0, 1.0)

    /** The whole percentage of a weight. */
    fun percent(weight: Double): Int = (weight * PERCENT).roundToInt()

    /** Sorted by weight, highest first; entities without a weight keep their order after the weighted ones. */
    fun sorted(entities: List<MonarchAiRelatedEntity>): List<MonarchAiRelatedEntity> =
        entities.withIndex()
            .sortedWith(compareByDescending<IndexedValue<MonarchAiRelatedEntity>> { weight(it.value.weight) ?: -1.0 }
                .thenBy { it.index })
            .map { it.value }

    /** The relations of [id] as the other entity and the relation label, skipping unknown ids. */
    fun relationsOf(
        id: String,
        entities: List<MonarchAiRelatedEntity>,
        relations: List<MonarchAiEntityRelation>,
    ): List<Pair<String, String?>> {
        val byId = entities.associateBy { it.id }
        return relations.mapNotNull { relation ->
            val otherId = when (id) {
                relation.from -> relation.to
                relation.to -> relation.from
                else -> null
            }
            otherId?.let(byId::get)?.let { it.label to relation.label }
        }
    }

    /** Whether the graph is drawn: asked for, not empty, not too many entities and not at a large font size. */
    fun showsGraph(
        variant: MonarchAiRelatedEntitiesVariant,
        count: Int,
        maxGraphNodes: Int,
        fontScale: Float,
    ): Boolean =
        variant == MonarchAiRelatedEntitiesVariant.Graph && count in 1..maxGraphNodes &&
            fontScale <= GRAPH_MAX_FONT_SCALE

    /** The centre entity: [centerId] when known, else the first of [sorted]. */
    fun center(sorted: List<MonarchAiRelatedEntity>, centerId: String?): MonarchAiRelatedEntity? =
        sorted.firstOrNull { it.id == centerId } ?: sorted.firstOrNull()

    /** The point [index] of [count] on a circle of [radius] around the origin, from the top, clockwise. */
    fun ringPoint(index: Int, count: Int, radius: Float): Offset {
        val angle = -PI / 2 + 2 * PI * index / count
        return Offset((radius * cos(angle)).toFloat(), (radius * sin(angle)).toFloat())
    }
}
