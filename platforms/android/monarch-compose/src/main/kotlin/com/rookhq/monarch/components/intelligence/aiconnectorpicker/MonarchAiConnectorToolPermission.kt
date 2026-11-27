package com.rookhq.monarch.components.intelligence.aiconnectorpicker

/** Whether the assistant may run a connector's tool. Same names as iOS. */
public enum class MonarchAiConnectorToolPermission {
    /** Runs without asking. */
    Allow,

    /** Asks the person each time. */
    Ask,

    /** Never runs. */
    Never,
}
