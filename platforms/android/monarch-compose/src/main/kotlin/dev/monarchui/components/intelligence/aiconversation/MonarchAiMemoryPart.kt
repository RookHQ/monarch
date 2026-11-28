package dev.monarchui.components.intelligence.aiconversation

/**
 * A memory the assistant used, saved or forgot while writing a reply. The
 * `Memory` stream event carries the same value.
 *
 * @property id the memory's id in your memory store.
 * @property action what happened to the memory.
 * @property text the remembered fact, in plain language.
 * @property scope where the memory applies, for example `user`, `project` or a workspace name.
 */
public data class MonarchAiMemoryPart(
    public val id: String,
    public val action: MonarchAiMemoryAction,
    public val text: String,
    public val scope: String? = null,
) : MonarchAiPart {
    override val type: String get() = "memory"
}
