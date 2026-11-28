package com.rookhq.monarch.components.intelligence.aiartifactversions

import androidx.compose.runtime.Immutable

/**
 * Replacements for the strings `MonarchAiArtifactVersions` supplies. Each
 * `null` keeps the built-in English string `monarch_aiartifactversions_<key>`.
 *
 * @property label the TalkBack name of the controls. Default "Versions".
 * @property position the position from the version number and the count. Default "Version 3 of 5".
 * @property previous the TalkBack name of the previous button. Default "Previous version".
 * @property next the TalkBack name of the next button. Default "Next version".
 * @property select the menu button that jumps to any version. Default: the shown version, for example
 * "v3 · Added risks".
 * @property versionTag the short tag of a version in the menu. Default "v3".
 * @property versionName a version's name in the diff. Default "Version 3".
 * @property compare the toggle that shows the diff with the previous version. Default "Compare with previous".
 * @property announce announced when the menu changes the version. Default "Showing version 3 of 5".
 */
@Immutable
public class MonarchAiArtifactVersionsLabels(
    public val label: String? = null,
    public val position: ((version: Int, total: Int) -> String)? = null,
    public val previous: String? = null,
    public val next: String? = null,
    public val select: String? = null,
    public val versionTag: ((version: Int) -> String)? = null,
    public val versionName: ((version: Int) -> String)? = null,
    public val compare: String? = null,
    public val announce: ((version: Int, total: Int) -> String)? = null,
) {
    override fun equals(other: Any?): Boolean =
        other is MonarchAiArtifactVersionsLabels && other.label == label && other.position == position &&
            other.previous == previous && other.next == next && other.select == select &&
            other.versionTag == versionTag && other.versionName == versionName && other.compare == compare &&
            other.announce == announce

    override fun hashCode(): Int =
        listOf(label, position, previous, next, select, versionTag, versionName, compare, announce).hashCode()
}
