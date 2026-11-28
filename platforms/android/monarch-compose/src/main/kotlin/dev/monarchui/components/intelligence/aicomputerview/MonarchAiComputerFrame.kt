package dev.monarchui.components.intelligence.aicomputerview

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.painter.Painter

/**
 * The latest screenshot of a `MonarchAiComputerView`.
 *
 * @property image the screenshot, loaded by the app; `null` shows "Screenshot unavailable".
 * @property width the width of the screenshot in pixels; with [height] it sets the box and places the markers.
 * @property height the height of the screenshot in pixels.
 * @property capturedAt when the screenshot was taken, in epoch milliseconds.
 * @property alt what the screenshot shows, in words.
 * @property sensitive the screenshot may show private information: it is never shown.
 */
@Stable
public class MonarchAiComputerFrame(
    public val image: Painter?,
    public val width: Int,
    public val height: Int,
    public val capturedAt: Long,
    public val alt: String? = null,
    public val sensitive: Boolean = false,
)
