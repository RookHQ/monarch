package com.rookhq.monarch.components.intelligence.aiconversation

/**
 * One piece of a message. A message is an ordered list of parts, so text,
 * reasoning, tool calls, sources and artifacts appear in the order the model
 * produced them. Each part reads and writes the web's JSON: its fields and a
 * `type` field that names it.
 */
public sealed interface MonarchAiPart {
    /** The name of the part in the web model: `text`, `tool-call`, `edit-proposal` and so on. */
    public val type: String
}
