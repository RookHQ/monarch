package dev.monarchui.components.intelligence.aiconversation

/**
 * The fields of a tool call that a `ToolUpdate` stream event changes. A field that is `null` is left as
 * it is.
 *
 * @property name the new tool name.
 * @property input the new arguments.
 * @property state the new state.
 * @property output what the tool returned.
 * @property error why the call failed or was denied.
 * @property startedAt the ISO 8601 time the tool started.
 * @property endedAt the ISO 8601 time the tool ended.
 * @property inputText the arguments as raw JSON text.
 * @property risk how much harm running the tool can do.
 */
public data class MonarchAiToolCallPatch(
    public val name: String? = null,
    public val input: MonarchAiJSON? = null,
    public val state: MonarchAiToolCallState? = null,
    public val output: MonarchAiJSON? = null,
    public val error: String? = null,
    public val startedAt: String? = null,
    public val endedAt: String? = null,
    public val inputText: String? = null,
    public val risk: MonarchAiToolCallRisk? = null,
) {
    /** The tool call with the patch applied. */
    internal fun applyTo(call: MonarchAiToolCallPart): MonarchAiToolCallPart = call.copy(
        name = name ?: call.name,
        input = input ?: call.input,
        state = state ?: call.state,
        output = output ?: call.output,
        error = error ?: call.error,
        startedAt = startedAt ?: call.startedAt,
        endedAt = endedAt ?: call.endedAt,
        inputText = inputText ?: call.inputText,
        risk = risk ?: call.risk,
    )
}
