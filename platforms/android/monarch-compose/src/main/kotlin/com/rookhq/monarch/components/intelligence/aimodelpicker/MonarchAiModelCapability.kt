package com.rookhq.monarch.components.intelligence.aimodelpicker

/** What an AI model can do, shown as a badge in `MonarchAiModelPicker`. */
public enum class MonarchAiModelCapability {
    /** Reads images. */
    Vision,

    /** Calls tools. */
    Tools,

    /** Reasons step by step before answering. */
    Reasoning,

    /** Searches the web. */
    Web,
}
