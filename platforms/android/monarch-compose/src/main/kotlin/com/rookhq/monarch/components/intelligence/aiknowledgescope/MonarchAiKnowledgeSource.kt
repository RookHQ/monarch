package com.rookhq.monarch.components.intelligence.aiknowledgescope

import androidx.compose.runtime.Immutable

/**
 * One source a `MonarchAiKnowledgeScope` offers. A source with [children] is a
 * group, such as a space with its documents.
 *
 * @property id a stable id, unique in the whole tree. It is what the selection holds.
 * @property label the visible name, for example "Handbook".
 * @property kind a space, a document or an app; written next to the name.
 * @property description a short second line, for example "24 documents".
 * @property children the sources inside this one. Empty for a single source.
 */
@Immutable
public class MonarchAiKnowledgeSource(
    public val id: String,
    public val label: String,
    public val kind: MonarchAiKnowledgeSourceKind,
    public val description: String? = null,
    public val children: List<MonarchAiKnowledgeSource> = emptyList(),
) {
    /** Whether the source is a group: it has children. */
    public val isGroup: Boolean get() = children.isNotEmpty()
}
