package com.rookhq.monarch.components.intelligence.aiconversation

/** The result of a tool the client ran. */
public sealed interface MonarchAiToolResult {
    /** The tool finished with this output. The call becomes `Succeeded`. */
    public data class Output(public val output: MonarchAiJSON) : MonarchAiToolResult

    /** The tool failed with this message, and perhaps partial output. The call becomes `Failed`. */
    public data class Failure(
        public val message: String,
        public val output: MonarchAiJSON? = null,
    ) : MonarchAiToolResult
}
