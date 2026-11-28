package dev.monarchui.components.intelligence.aiconnectorpicker

/** What a connector's tool can do to the person's data. Same names as iOS. */
public enum class MonarchAiConnectorToolRisk {
    /** Reads data only. */
    Read,

    /** Creates or changes data. */
    Write,

    /** Can delete data. */
    Destructive,
}
