package com.rookhq.monarch.components.intelligence.airelatedentities

/** How a `MonarchAiRelatedEntities` lays out the entities. The same names exist on every platform. */
public enum class MonarchAiRelatedEntitiesVariant {
    /** A ranked list. The default, and the accessible form of the graph. */
    List,

    /** A radial diagram with the centre entity in the middle. Falls back to the list when it does not fit. */
    Graph,
}
