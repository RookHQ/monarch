package dev.monarchui.components.intelligence.aiconversation

/**
 * A call the model made to a tool.
 *
 * @property id the id of the call.
 * @property name the tool name as the model called it, for example `search_documents`.
 * @property input the arguments.
 * @property state where the call is in its lifecycle.
 * @property output what the tool returned.
 * @property error why the call failed or was denied.
 * @property startedAt the ISO 8601 time the tool started.
 * @property endedAt the ISO 8601 time the tool ended.
 * @property inputText the arguments as raw JSON text while they stream, before [input] is parsed.
 * @property risk how much harm running it can do, when the service says.
 */
public data class MonarchAiToolCallPart(
    public val id: String,
    public val name: String,
    public val input: MonarchAiJSON = MonarchAiJSON.Null,
    public val state: MonarchAiToolCallState,
    public val output: MonarchAiJSON? = null,
    public val error: String? = null,
    public val startedAt: String? = null,
    public val endedAt: String? = null,
    public val inputText: String? = null,
    public val risk: MonarchAiToolCallRisk? = null,
) : MonarchAiPart {
    override val type: String get() = "tool-call"
}
