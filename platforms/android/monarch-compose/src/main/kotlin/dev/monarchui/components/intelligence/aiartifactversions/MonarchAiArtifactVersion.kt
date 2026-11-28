package dev.monarchui.components.intelligence.aiartifactversions

import androidx.compose.runtime.Immutable

/**
 * One saved version of an AI artifact, as `MonarchAiArtifactVersions` steps
 * through it.
 *
 * @property version the version number, counting from 1.
 * @property createdAt when it was made, in epoch milliseconds (UTC).
 * @property label an optional name, such as "Shorter intro".
 * @property content the version's text, compared with the previous version's. Without it Compare is not offered
 * for this version or the next.
 */
@Immutable
public data class MonarchAiArtifactVersion(
    public val version: Int,
    public val createdAt: Long,
    public val label: String? = null,
    public val content: String? = null,
)
