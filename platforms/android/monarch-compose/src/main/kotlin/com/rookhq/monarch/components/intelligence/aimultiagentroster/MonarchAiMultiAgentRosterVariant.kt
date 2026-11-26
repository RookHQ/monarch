package com.rookhq.monarch.components.intelligence.aimultiagentroster

/** How a `MonarchAiMultiAgentRoster` shows its agents. */
public enum class MonarchAiMultiAgentRosterVariant {
    /** A row per agent with its picture, name, role, task and state. */
    List,

    /** Overlapping pictures beside the summary, for headers and toolbars. */
    Stack,
}
