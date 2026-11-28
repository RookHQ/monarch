package dev.monarchui.components.intelligence.aitonepicker

/** How `MonarchAiTonePicker` shows its tones. The same names exist on every platform. */
public enum class MonarchAiTonePickerVariant {
    /** One card per tone, with its description and a radio mark. The default. */
    Cards,

    /** A field that opens a menu of the tone names, for toolbars and tight spaces. */
    Compact,
}
