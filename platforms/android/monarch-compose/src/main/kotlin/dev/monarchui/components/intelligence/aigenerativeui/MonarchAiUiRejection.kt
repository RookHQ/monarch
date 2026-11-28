package dev.monarchui.components.intelligence.aigenerativeui

/** Why `MonarchAiGenerativeUi` did not render a part. */
public enum class MonarchAiUiRejection {
    /** The component name is not in the registry. */
    UnknownComponent,

    /**
     * The props are not an object of plain data, the registered decoder
     * returned `null` or threw, or the validator refused them.
     */
    InvalidProps,
}
