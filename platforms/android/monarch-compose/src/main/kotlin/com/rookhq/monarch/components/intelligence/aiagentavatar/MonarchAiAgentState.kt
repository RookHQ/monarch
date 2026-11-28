package com.rookhq.monarch.components.intelligence.aiagentavatar

/** What an AI agent is doing, shown by `MonarchAiAgentAvatar` as a ring and in words. */
public enum class MonarchAiAgentState {
    /** Not running. No ring. */
    Idle,

    /** Running. A turning ring in the AI colours, still when animations are off. */
    Working,

    /** Stopped until a person answers or approves. A dashed ring in the warning colour. */
    Waiting,

    /** Stopped with an error. A ring and a badge in the danger colour. */
    Error,
}
