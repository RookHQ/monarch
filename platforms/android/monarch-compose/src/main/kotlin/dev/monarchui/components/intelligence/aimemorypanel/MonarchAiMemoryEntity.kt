package dev.monarchui.components.intelligence.aimemorypanel

/**
 * Who or what a memory is about; `MonarchAiMemoryPanel` groups memories by it.
 *
 * @property kind a person, an organization, a project or other.
 * @property name the name shown as the group heading, such as "Ada Lovelace".
 */
public data class MonarchAiMemoryEntity(
    public val kind: MonarchAiMemoryEntityKind,
    public val name: String,
)
