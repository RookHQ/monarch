package com.rookhq.monarch.components.intelligence.aiconversation

/** Whether a text or reasoning part is still being written. */
public enum class MonarchAiPartState {
    /** More text is arriving. */
    Streaming,

    /** The part is complete. */
    Done,
}
