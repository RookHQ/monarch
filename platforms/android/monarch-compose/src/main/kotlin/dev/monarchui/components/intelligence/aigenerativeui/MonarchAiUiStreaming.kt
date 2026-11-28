package dev.monarchui.components.intelligence.aigenerativeui

/** How a registered component renders while its props are still streaming. */
public enum class MonarchAiUiStreaming {
    /**
     * Render what has arrived as soon as it decodes. The default. Let the
     * decoder accept props that arrive late as missing, so the component can
     * show early.
     */
    Progressive,

    /** Show a progress indicator until the part is complete. */
    OnComplete,
}
