package com.rookhq.monarch.components.intelligence.aistructuredanswer

/** How a `MonarchAiStructuredAnswer` lays out its fields. */
public enum class MonarchAiStructuredAnswerLayout {
    /** One field per row, the label beside the value where there is room and above it otherwise. */
    List,

    /** The label above the value, two fields per row from 640dp wide; lists and Markdown take a whole row. */
    Grid,
}
