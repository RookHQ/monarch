package dev.monarchui.components.intelligence.aiconversation

/**
 * The agent that answered, from the stream's `Agent` event.
 *
 * @property id a stable id, for example `dossier-assistant`.
 * @property name the display name.
 * @property codename a short codename shown in compact places.
 * @property role what the agent does, for example "Writing assistant".
 * @property avatar the URL of the agent's avatar image. Monarch loads no images; load it in the app.
 */
public data class MonarchAiAgentRef(
    public val name: String,
    public val id: String? = null,
    public val codename: String? = null,
    public val role: String? = null,
    public val avatar: String? = null,
)
