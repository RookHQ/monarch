package dev.monarchui.components.intelligence.aiconversation

/**
 * Generative UI: a component the reply asks to show. Render it only from an
 * allow-listed registry, and treat the props as untrusted data.
 *
 * @property component the name of the component in the registry.
 * @property props the props, or, while [state] is `Streaming`, JSON text that is still arriving.
 * @property id a stable id, so actions from the component can name the part.
 * @property state `Streaming` while the props are still arriving.
 * @property toolCallId the tool call this UI answers, so an action can be sent back as its result.
 */
public data class MonarchAiUiPart(
    public val component: String,
    public val props: MonarchAiJSON,
    public val id: String? = null,
    public val state: MonarchAiPartState? = null,
    public val toolCallId: String? = null,
) : MonarchAiPart {
    override val type: String get() = "ui"
}
