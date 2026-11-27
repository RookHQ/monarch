package com.rookhq.monarch.components.datadisplay.statusbadge

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The statuses of a product, by key, for `MonarchStatusBadge`.
 *
 * A badge looks a key up in its own registry, then in the registry provided
 * through [LocalMonarchStatusRegistry], then in the built-in set
 * (`operational`, `degraded`, `partial`, `down`, `maintenance`, `unknown`,
 * `success`, `warning`, `danger`, `info`, `neutral`). A key found nowhere is
 * shown in the neutral tone with a readable label made from the key.
 *
 * @property definitions the statuses by key.
 */
@Immutable
public class MonarchStatusRegistry(public val definitions: Map<String, MonarchStatusDefinition>) {
    /** The definition of [key] in this registry, or `null` when it has none. */
    public fun resolve(key: String?): MonarchStatusDefinition? = key?.let { definitions[it] }

    /** A registry with the statuses of this one and of [other]; [other] wins where both define a key. */
    public fun merging(other: MonarchStatusRegistry): MonarchStatusRegistry =
        MonarchStatusRegistry(definitions + other.definitions)

    public companion object {
        /** A registry with no statuses: only the built-in set applies. */
        public val Empty: MonarchStatusRegistry = MonarchStatusRegistry(emptyMap())

        /**
         * A registry typed by an enumeration: each key is the enumeration constant's [Enum.name].
         *
         * @param definitions the statuses by constant.
         */
        public fun <K : Enum<K>> of(definitions: Map<K, MonarchStatusDefinition>): MonarchStatusRegistry =
            MonarchStatusRegistry(definitions.mapKeys { it.key.name })
    }
}

/**
 * The registry every `MonarchStatusBadge` below it looks keys up in, before the
 * built-in set. Provide it once near the root of a screen; to add to an outer
 * registry, provide `LocalMonarchStatusRegistry.current.merging(…)`.
 */
public val LocalMonarchStatusRegistry: ProvidableCompositionLocal<MonarchStatusRegistry> =
    staticCompositionLocalOf { MonarchStatusRegistry.Empty }
