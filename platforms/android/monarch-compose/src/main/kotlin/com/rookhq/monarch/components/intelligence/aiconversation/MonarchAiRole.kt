package com.rookhq.monarch.components.intelligence.aiconversation

/** Who wrote a message. In the web's JSON it is the lower-case name. */
public enum class MonarchAiRole {
    /** The person. */
    User,

    /** The model. */
    Assistant,

    /** Instructions to the model. */
    System,

    /** The result of a tool. */
    Tool,
}
