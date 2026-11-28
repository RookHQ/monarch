package com.rookhq.monarch.components.intelligence.airelatedentities

import androidx.compose.runtime.Immutable

/**
 * What a related entity is. The common kinds are constants with built-in
 * names; any other value is allowed and shown as given, as on the web.
 *
 * @property value the kind's identifier, for example `person`.
 */
@Immutable
public class MonarchAiRelatedEntityKind(public val value: String) {
    override fun equals(other: Any?): Boolean = other is MonarchAiRelatedEntityKind && other.value == value

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value

    /** The common kinds, the same on every platform. */
    public companion object {
        /** A person. */
        public val Person: MonarchAiRelatedEntityKind = MonarchAiRelatedEntityKind("person")

        /** A company or another organisation. */
        public val Org: MonarchAiRelatedEntityKind = MonarchAiRelatedEntityKind("org")

        /** A project. */
        public val Project: MonarchAiRelatedEntityKind = MonarchAiRelatedEntityKind("project")

        /** A document. */
        public val Document: MonarchAiRelatedEntityKind = MonarchAiRelatedEntityKind("document")

        /** A record such as a deal or a ticket. */
        public val Record: MonarchAiRelatedEntityKind = MonarchAiRelatedEntityKind("record")

        /** A place. */
        public val Place: MonarchAiRelatedEntityKind = MonarchAiRelatedEntityKind("place")

        /** Anything else. */
        public val Other: MonarchAiRelatedEntityKind = MonarchAiRelatedEntityKind("other")
    }
}
